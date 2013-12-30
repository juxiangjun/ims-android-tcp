package com.eme.ims.audio;

	
public class Encoder implements Runnable {

	private volatile int leftSize = 0;
	private final Object mutex = new Object();
	private SpeexCodec speex = null;//new SpeexCodec();
	private long ts;
	private Consumer consumer;
	private byte[] processedData = new byte[1024];
	private short[] rawdata = new short[1024];
	private volatile boolean isRecording;
	private static int bufferSize = 0;
	
	private static Encoder encoder;
	
	public static Encoder getInstance(Consumer consumer, SpeexCodec speex) {
		if (encoder == null) {
			encoder = new Encoder(consumer, speex);
		}
		return encoder;
	}
	
	private Encoder(Consumer consumer, SpeexCodec speex) {
		super();
		this.consumer = consumer;
		this.speex = speex;
		//speex.init();
	}

	public void run() {

		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

		int getSize = 0;
		while (this.isRecording()) {
			if (isIdle()) {
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			synchronized (mutex) {
//				int packageSize = bufferSize / 160;
//				int remain = bufferSize % 160;
//				int i=0;
//				for (; i<packageSize; i++) {
//					short[] tmp = new short[160];
//					System.arraycopy(rawdata, i*160, tmp, 0, 160);
//					getSize = speex.encode(tmp, 0, processedData, 160);
//					consumer.putData(ts, processedData, getSize);
//				}
//				
//				short[] tmp = new short[remain];
//				System.arraycopy(rawdata, i*160, tmp, 0, remain);
//				System.arraycopy(tmp, 0, rawdata, 0, remain);
//				bufferSize = remain;
				getSize = speex.encode(rawdata, 0, processedData, leftSize);
				//getSize = aacEncoder.encode(rawdata, processedData);
				setIdle();
			}
			if (getSize > 0) {
				consumer.putData(ts, processedData, getSize);
			}
		}
	}

	public void putData(long ts, short[] data, int size) {
		synchronized (mutex) {
			this.ts = ts;
			System.arraycopy(data, 0, rawdata, 0, size);
			//bufferSize = bufferSize + size;
			this.leftSize = size;
		}
	}

	public boolean isIdle() {
		synchronized (mutex) {
			return leftSize == 0 ? true : false;
		}
	}

	public void setIdle() {
		leftSize = 0;
	}

	public void setRecording(boolean isRecording) {
		synchronized (mutex) {
			this.isRecording = isRecording;
		}
	}

	public boolean isRecording() {
		synchronized (mutex) {
			return isRecording;
		}
	}
}

