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
	private List<byte[]> list;
	
	private static int PACKAGES_TO_PUBLISH = 32;
	
	private static Boolean FINISHED_PUBLISH = true;
	
	
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
		
		list = Collections.synchronizedList(new LinkedList<byte[]>());
		//list = new ArrayList<byte[]>();
	}
	
	@Override
	public void putData(long ts, byte[] buf, int size) {
		//processedData data = new processedData();
		byte[] decodedData = new byte[size];
		System.arraycopy(buf, 0, decodedData, 0, size);
		list.add(decodedData);
	}
	
	@Override
	public void run() {
		
		while (this.isRunning()) {
			synchronized (mutex) {
				startPcmRecorder();
				while (!this.isRecording) {
					try {
						mutex.wait();
					} catch (InterruptedException e) {
						throw new IllegalStateException("Wait() interrupted!", e);
					}
				}
			}
			
			while (this.isRecording()) {
				synchronized(FINISHED_PUBLISH) {
					if (list.size() > PACKAGES_TO_PUBLISH && FINISHED_PUBLISH) {
						publish();
						//Log.d(LOG_TAG, "list size = "+list.size());
					} else {
						try {
							Thread.sleep(20);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
			recorder.stop();
			if (list.size() >8){
				//publish();
				Log.d(LOG_TAG, "list size = "+list.size());
			} else {
				
			}
			stop();
		}
	}
	
	private void startPcmRecorder(){
		recorder = PcmRecorder.getInstance(this, speex);
		recorder.setRecording(true);
		Thread th = new Thread(recorder);
		th.start();
	}
	
	private void publish() {
		synchronized(FINISHED_PUBLISH) {
			FINISHED_PUBLISH = false;
			Message msg = new Message();
			msg.setFrom("d63d65fb-78c8-40f6-9fcc-afc12b352823");
			msg.setTo("d63d65fb-78c8-40f6-9fcc-afc12b352823");
			msg.setGroupId("00000-00000-00000-00000-00000-000000");
			msg.setCommandId(MsgProtocol.Command.SEND_P2P_MESSAGE);
			msg.setType(MsgProtocol.MsgType.AUDIO_STREAM);
			msg.setDirection(MsgProtocol.MsgDirection.CLIENT_TO_SERVER);
			byte[] tmp = new byte[1024];
			int n = 0;
			
			for (int i=0; i<PACKAGES_TO_PUBLISH; i++) {
				//System.out.print("list size:" + list.size());
				byte[] data = list.remove(0);
				System.arraycopy(data, 0, tmp, n, data.length);
				n = n + data.length;
			}
			
			byte[] contents = new byte[n];
			System.arraycopy(tmp, 0, contents, 0, n);
			msg.setContents(contents);
			messageClient.sendMessage(msg);
			FINISHED_PUBLISH = true;
		}
		
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
