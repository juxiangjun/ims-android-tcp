package com.eme.ims.manager;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.mina.core.service.IoHandlerAdapter;

import android.content.Context;
import android.util.Log;

import com.eme.ims.audio.Consumer;
import com.eme.ims.audio.PcmRecorder;
import com.eme.ims.audio.SpeexCodec;
import com.eme.ims.client.ClientHandler;
import com.eme.ims.client.MessageClient;
import com.eme.ims.codec.Message;
import com.eme.ims.codec.MsgProtocol;
import com.eme.ims.handler.IMessageAdapter;
import com.eme.ims.utils.PropertyConfig;

public class AudioTalkerManager implements Runnable, Consumer {

	private static final String LOG_TAG = "AudioTalkerManager";
	private PcmRecorder recorder = null;
	
	/**数据处理类型*/
	public static final short TO_SERVER = 1;
	public static final short TO_FILE = 2;
	public static final short BOTH = 3;
	
	/**跟踪录音机状态*/
	private volatile boolean isRecording;
	private volatile boolean isRunning;
	private final Object mutex = new Object();
	private List<processedData> list;
	
	
	private PropertyConfig config;
	private Context ctx;
	private static AudioTalkerManager audioTalkerManager;
	private static MessageClient messageClient;
	private static UIViewFactory factory;
	private SpeexCodec speex;
	
	
	public static AudioTalkerManager getInstance(Context ctx,  PropertyConfig config, IMessageAdapter adapter, SpeexCodec speex) {
		if (audioTalkerManager == null) {
			audioTalkerManager = new AudioTalkerManager (ctx, config, adapter, speex);
		}
		return audioTalkerManager;
	}
	
	public MessageClient getMessageClient() {
		return messageClient;
	}
	
	private AudioTalkerManager(Context ctx, PropertyConfig config, IMessageAdapter adapter, SpeexCodec speex) {
		super();
		this.config = config;
		this.ctx = ctx;
		this.speex = speex;
		factory = UIViewFactory.INSTANCE;
		IoHandlerAdapter handler = new ClientHandler(adapter);
		messageClient = new MessageClient(config, handler);
		messageClient.connect();
		
		Message msg = new Message();
		msg.setFrom("d63d65fb-78c8-40f6-9fcc-afc12b352823");
		msg.setTo("d63d65fb-78c8-40f6-9fcc-afc12b352823");
		msg.setGroupId("00000-00000-00000-00000-00000-000000");
		msg.setCommandId(MsgProtocol.Command.REGISTRATION);
		msg.setType(MsgProtocol.MsgType.TEXT);
		msg.setDirection(MsgProtocol.MsgDirection.CLIENT_TO_SERVER);
		messageClient.sendMessage(msg);
		
		list = Collections.synchronizedList(new LinkedList<processedData>());
	}
	
	@Override
	public void putData(long ts, byte[] buf, int size) {
		processedData data = new processedData();
		System.arraycopy(buf, 0, data.processed, 0, size);
		list.add(data);
	}
	
	@Override
	public void run() {
		while (this.isRunning()) {
			synchronized (mutex) {
				while (!this.isRecording) {
					try {
						mutex.wait();
					} catch (InterruptedException e) {
						throw new IllegalStateException("Wait() interrupted!", e);
					}
				}
			}
			startPcmRecorder();
			while (this.isRecording()) {
				if (list.size() > 0) {
					publish();
					Log.d(LOG_TAG, "list size = "+list.size());
				} else {
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			recorder.stop();
			while(list.size() > 0){
				publish();
				Log.d(LOG_TAG, "list size = "+list.size());
			}
			stop();
		}
	}
	
	private void startPcmRecorder(){
		recorder = new PcmRecorder(this, this.speex);
		recorder.setRecording(true);
		Thread th = new Thread(recorder);
		th.start();
	}
	
	private void publish() {
		processedData data = list.remove(0);
		Message msg = new Message();
		msg.setFrom("d63d65fb-78c8-40f6-9fcc-afc12b352823");
		msg.setTo("d63d65fb-78c8-40f6-9fcc-afc12b352823");
		msg.setGroupId("00000-00000-00000-00000-00000-000000");
		msg.setCommandId(MsgProtocol.Command.SEND_P2P_MESSAGE);
		msg.setType(MsgProtocol.MsgType.AUDIO_STREAM);
		msg.setDirection(MsgProtocol.MsgDirection.CLIENT_TO_SERVER);
		msg.setContents(data.processed);
		messageClient.sendMessage(msg);
	}
	
	public void stop() {
		this.isRecording = false;
	}
	
	public boolean isRunning() {
		synchronized (mutex) {
			return isRunning;
		}
	}
	
	public void setRunning(boolean isRunning) {
		synchronized (mutex) {
			this.isRunning = isRunning;
			if (this.isRunning) {
				mutex.notify();
			}
		}
	}

	public void setRecording(boolean isRecording) {
		synchronized (mutex) {
			this.isRecording = isRecording;
			if (this.isRecording) {
				mutex.notify();
			}
		}
	}

	public boolean isRecording() {
		synchronized (mutex) {
			return isRecording;
		}
	}
	
	
	class processedData {
		private long ts;
		private int size;
		private byte[] processed = new byte[256];
	}
}
