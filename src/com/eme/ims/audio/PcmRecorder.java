package com.eme.ims.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class PcmRecorder implements Runnable {

	private volatile boolean isRecording;
	private final Object mutex = new Object();
	private static final int frequency = 8000;
	private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private Consumer consumer;
	private SpeexCodec speex;
//	private static AudioRecord recordInstance;
//	private static int bufferSize = 0;
//	private static int bufferRead = 0;
	
	private static PcmRecorder pcmRecorder = null;
	
	public static PcmRecorder getInstance(Consumer consumer, SpeexCodec speex) {
		if (pcmRecorder == null) {
			pcmRecorder = new PcmRecorder(consumer, speex);
		}
		return pcmRecorder;
	}
	
	
	private PcmRecorder(Consumer consumer, SpeexCodec speex) {
		super();
		this.consumer = consumer;
		this.speex = speex;
	}

	public void run() {

		Encoder encoder = Encoder.getInstance(consumer, speex);
		Thread encodeThread = new Thread(encoder);
		encoder.setRecording(true);
		encodeThread.start();

		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

		int bufferRead = 0;
		int bufferSize = AudioRecord.getMinBufferSize(frequency,
				AudioFormat.CHANNEL_IN_MONO, audioEncoding);

		short[] tempBuffer = new short[bufferSize];
		
		AudioRecord recordInstance = new AudioRecord(
				MediaRecorder.AudioSource.MIC, frequency,
				AudioFormat.CHANNEL_IN_MONO, audioEncoding, bufferSize);

		recordInstance.startRecording();

		while (this.isRecording) {

			bufferRead = recordInstance.read(tempBuffer, 0, 160);
			//bufferRead = recordInstance.read(tempBuffer, 0, 640);
			
			if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
				throw new IllegalStateException(
						"read() returned AudioRecord.ERROR_INVALID_OPERATION");
			} else if (bufferRead == AudioRecord.ERROR_BAD_VALUE) {
				throw new IllegalStateException(
						"read() returned AudioRecord.ERROR_BAD_VALUE");
			} else if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
				throw new IllegalStateException(
						"read() returned AudioRecord.ERROR_INVALID_OPERATION");
			}

			if (encoder.isIdle()) {
				encoder.putData(System.currentTimeMillis(), tempBuffer, bufferRead);
			} else {
				
			}

		}
		recordInstance.stop();
		encoder.setRecording(false);
	}

	public void stop(){
		setRecording(false);
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
}
