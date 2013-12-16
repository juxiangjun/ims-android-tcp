package com.eme.ims;

import java.io.InputStream;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.eme.ims.audio.AudioQueuePlayer;
import com.eme.ims.audio.SpeexCodec;
import com.eme.ims.codec.Message;
import com.eme.ims.codec.MsgProtocol;
import com.eme.ims.handler.IMessageAdapter;
import com.eme.ims.manager.AudioManager;
import com.eme.ims.manager.AudioTalkerManager;
import com.eme.ims.manager.ChatManager;
import com.eme.ims.manager.UIViewFactory;
import com.eme.ims.utils.PropertyConfig;

public class ImsMainActivity extends Activity implements IMessageAdapter {
	
	
	private AudioManager audioManager;
	private ChatManager chatManager;
	
	private MediaRecorder mRecorder;
	private MediaPlayer mPlayer;
	
	private Button mPlayButton;
	private Button mRecordButton;
	private Button mDeleteButton;
	private LinearLayout llMessage;
	
	private static final String LOG_TAG = "ImsMainActivity";
    
	private PropertyConfig config;
	private static Handler handler = new Handler();
	
	private boolean stopRecording = true;
	
	public static final int STOPPED = 0;
	public static final int RECORDING = 1;
	int status = STOPPED;
	
	
	private UIViewFactory factory= null;
	private AudioTalkerManager audioTalkerManager ;
	private AudioQueuePlayer audioQueuePlayer;
	private Button btnTalk;
	
	private SpeexCodec speex = new SpeexCodec();
	
    @Override
    protected void onCreate (Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        factory = UIViewFactory.INSTANCE;
        InputStream tmp = null;
        try {
        	speex.init();
			tmp = this.getResources().openRawResource(R.raw.client);
			config = new PropertyConfig(tmp);
			audioTalkerManager = AudioTalkerManager.getInstance(this, config, this, speex);
			audioTalkerManager.setRunning(true);
			btnTalk = factory.createTextButton(this, "Start", this.getBtnTalkerClickListner());
			
			audioQueuePlayer = AudioQueuePlayer.getInstance();
			
			Thread thread = new Thread(audioTalkerManager);
			thread.start();
			
			
			Thread audioPlayerThread = new Thread(audioQueuePlayer);
			audioPlayerThread.start();
			
			if (config.getKeyCount()==0) {
				Log.e(LOG_TAG, "did'n find configuration files. this app exits.");
				System.exit(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
//        message = new Message();
//        message.setFrom(config.getString("from"));
//        //message.setTo(config.getString("to"));
//        message.setTo("00000-00000-00000-00000-00000-000000");
//        message.setGroupId(config.getString("group_id"));
//        message.setDirection(MsgProtocol.MsgDirection.CLIENT_TO_SERVER);
       
//        
//        audioManager = AudioManager.getInstance(this, config);
//        chatManager = ChatManager.getInstance(this, this, this.config, message);
//        
//        this.mRecorder = audioManager.getMediaRecorder();
//        this.mPlayer = audioManager.getMediaPlayer();
        
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        
//        mRecordButton = this.audioManager.getRecorderButton();
//        ll.addView(mRecordButton,
//            new LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.WRAP_CONTENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT,
//                0));
//        
//        mPlayButton = this.audioManager.getPlayerButton();
//        ll.addView(mPlayButton,
//            new LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.WRAP_CONTENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT,
//                0));
//        
//        
//        mDeleteButton = this.audioManager.getDeleteButton();
//        ll.addView(mDeleteButton,
//                new LinearLayout.LayoutParams(
//                    ViewGroup.LayoutParams.WRAP_CONTENT,
//                    ViewGroup.LayoutParams.WRAP_CONTENT,
//                    0));
//        
//        llMessage = this.chatManager.getChatLinearLayout();
//        ll.addView(llMessage,
//        		new LinearLayout.LayoutParams(
//                        ViewGroup.LayoutParams.WRAP_CONTENT,
//                        ViewGroup.LayoutParams.WRAP_CONTENT,
//                        0));
        ll.addView(btnTalk,
        		new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0)
        		);
        
        setContentView(ll);
    }

    
    
    private LinearLayout createTalkerLinearLayout() {
		LinearLayout ll = new LinearLayout(this);
		Button btnTalker = factory.createTextButton(this, "Start", this.getBtnTalkerClickListner());
		ll.addView(btnTalker,
	            new LinearLayout.LayoutParams(
	                ViewGroup.LayoutParams.WRAP_CONTENT,
	                ViewGroup.LayoutParams.WRAP_CONTENT,
	                0));
		return ll;
	}
	
	private View.OnClickListener getBtnTalkerClickListner() {
		return new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (!stopRecording) {
					btnTalk.setText("Start");
					stopRecording = true;
					stopRecord();
				} else {
					btnTalk.setText("stop");
					stopRecording = false;
					startRecord();
				}
			}
		};
	}
    
	
	private void startRecord() {
		if(status == STOPPED){
			audioTalkerManager.setRecording(true);
			status = RECORDING;
		}
	}
	
	private void stopRecord() {
		if(status == RECORDING){
			audioTalkerManager.setRecording(false);
			status = STOPPED;
		}
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public void onPause() {
        super.onPause();
//        if (mRecorder != null) {
//            mRecorder.release();
//            mRecorder = null;
//        }
//
//        if (mPlayer != null) {
//            mPlayer.release();
//            mPlayer = null;
//        }
//        
//        if (chatManager != null) {
//        	//chatManager.disconnect();
//        }
    }
    
    
	@Override
	public void onReceivedP2PMessage(final Message message) {
		Log.d(LOG_TAG, "Enter onReceivedP2PMessage method.");
		handler.post(new Runnable() {
            @Override
            public void run() {
            	if (message.getType() == MsgProtocol.MsgType.AUDIO_STREAM) {
            		//audioManager.playOnlineAudio(new String(message.getContents()));
            		audioQueuePlayer.putData(message, speex);
            	}
            	Log.d(LOG_TAG, new String(message.getContents()));
            }
        });
	}

	@Override
	public void onGetP2PMessageResponse(final Message message) {
		Log.d("ImsMainActivity", "Enter onGetP2PMessageResponse method.");
		handler.post(new Runnable() {
            @Override
            public void run() {
            	Log.d(LOG_TAG, new String(message.getContents()));
            }
        });
	}
}
