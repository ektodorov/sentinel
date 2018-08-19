package com.example;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamMotionDetector;
import com.github.sarxos.webcam.WebcamMotionEvent;
import com.github.sarxos.webcam.WebcamMotionListener;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

public class Sentinel implements WindowListener {
	
	private Webcam mWebcam;
	private Recorder mRecorder;
	private WebcamMotionDetector mWebcamMotionDetector;
	private ExecutorService mExecutor;
	private ExecutorService mExecutorRecordVideo;
	private ScheduledExecutorService mScheduledExecutor;
	private SimpleDateFormat mDateFormat;
	private volatile boolean mIsSavingVideo;
	private boolean mIsMotionDetectionStarted;
	private Runnable mRunnableStopRecord;
	private Future<?> mFutureRecordVideo;
	
	private JRadioButton mRadioPictures;
	private JRadioButton mRadioVideos;
	private ButtonGroup mButtonGroupRadio;
	private JButton mButtonStartRecord;
	private JButton mButtonAbout;
	
	public Sentinel() {
		mRecorder = new Recorder();
		mExecutor = Executors.newSingleThreadExecutor();
		mExecutorRecordVideo = Executors.newSingleThreadExecutor();
		mScheduledExecutor = Executors.newSingleThreadScheduledExecutor();
		mDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
		mRunnableStopRecord = new Runnable() {
			@Override
			public void run() {
				mRecorder.recordStop();
			}
		};
	}
	
	public void createGui() {
		mWebcam = Webcam.getDefault();
		WebcamPanel panel = null;
		if(mWebcam != null) {
			mWebcam.setViewSize(WebcamResolution.VGA.getSize());

			panel = new WebcamPanel(mWebcam);
			//panel.setFPSDisplayed(true);
			//panel.setDisplayDebugInfo(true);
			//panel.setImageSizeDisplayed(true);
			panel.setMirrored(true);
		}

		JFrame window = new JFrame(ConstantsSentinel.STR_APPLICATION_NAME);
		//window.add(panel);
		window.setResizable(true);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.addWindowListener(this);
		
		Container contentPane = window.getContentPane();
		BoxLayout layout = new BoxLayout(contentPane, BoxLayout.Y_AXIS);
		contentPane.setLayout(layout);
		
		mRadioPictures = new JRadioButton("Record Pictures");
        mRadioVideos = new JRadioButton("Record Videos");
        mRadioPictures.setSelected(true);
        mButtonGroupRadio = new ButtonGroup();
        mButtonGroupRadio.add(mRadioPictures);
        mButtonGroupRadio.add(mRadioVideos);
		mButtonStartRecord = new JButton("Start");
		mButtonAbout = new JButton(ConstantsSentinel.STR_ABOUT);
		
		mButtonStartRecord.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(mIsMotionDetectionStarted) {
					mRecorder.recordStop();
					mExecutorRecordVideo.execute(new Runnable() {
						@Override
						public void run() {
							mIsMotionDetectionStarted = false;
							mButtonStartRecord.setText("Start");
						}
					});
				} else {
					mIsMotionDetectionStarted = true;
					mButtonStartRecord.setText("Stop");
				}
			}
		});
		
		mButtonAbout.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//JOptionPane.showMessageDialog(mFrame, ConstantsSentinel.STR_ABOUT_MESSAGE, ConstantsSentinel.STR_ABOUT_TITLE, JOptionPane.INFORMATION_MESSAGE);
				JFrame frame = new JFrame(ConstantsSentinel.STR_ABOUT_TITLE);
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				frame.setResizable(true);
				frame.setLocationRelativeTo(null);
				frame.setSize(768, 1024);
				JTextPane txt = new JTextPane();
				txt.setText(ConstantsSentinel.STR_ABOUT_MESSAGE);
				txt.setSize(768, 1024);
				JScrollPane jsp = new JScrollPane(txt);
				frame.add(jsp);
				frame.pack();
				frame.setVisible(true);
			}
		});
		
		if(panel != null) {contentPane.add(panel);}
		contentPane.add(mRadioPictures);
        contentPane.add(mRadioVideos);
        contentPane.add(mButtonStartRecord);
        contentPane.add(mButtonAbout);
		
		window.pack();
		window.setVisible(true);
		
		if(mWebcam != null) {
			mWebcamMotionDetector = new WebcamMotionDetector(mWebcam);
			mWebcamMotionDetector.setInterval(500);// check on 500 ms
			mWebcamMotionDetector.addMotionListener(new WebcamMotionListener() {
				@Override
				public void motionDetected(WebcamMotionEvent arg0) {
					if(!mIsMotionDetectionStarted) {return;}
				
					if(mRadioPictures.isSelected()) {
						recordImage();
					} else {
						recordVideo();
					}
				}
			});
			mWebcamMotionDetector.start();
		}
	}
	
	public void startMotionDetection() {
		mWebcamMotionDetector.start();
	}
	
	public void stopMotionDetection() {
		mWebcamMotionDetector.stop();
	}
	
	private void recordImage() {
		mExecutor.execute(new Runnable() {
			@Override
			public void run() {
				Date date = new Date();
				String fileName = mDateFormat.format(date);
				mRecorder.recordImageJpg(mWebcam, fileName);
			}
		});
	}
	
	private void recordVideo() {
		if(mFutureRecordVideo != null) {mFutureRecordVideo.cancel(false);}
		if(mIsSavingVideo) {
			mFutureRecordVideo = mScheduledExecutor.schedule(mRunnableStopRecord, 5, TimeUnit.SECONDS);
			return;
		}
		
		mIsSavingVideo = true;
		mExecutorRecordVideo.execute(new Runnable() {
			@Override
			public void run() {
				Date date = new Date();
				String fileName = mDateFormat.format(date);
				mRecorder.recordStart(mWebcam, fileName);
				mIsSavingVideo = false;
			}
		});
	}

	//WindowListener
	@Override
	public void windowOpened(WindowEvent e) {}

	@Override
	public void windowClosing(WindowEvent e) {
		if(mWebcamMotionDetector != null) {
			mWebcamMotionDetector.stop();
		}
		mRecorder.recordStop();
	}

	@Override
	public void windowClosed(WindowEvent e) {}

	@Override
	public void windowIconified(WindowEvent e) {}

	@Override
	public void windowDeiconified(WindowEvent e) {}

	@Override
	public void windowActivated(WindowEvent e) {}

	@Override
	public void windowDeactivated(WindowEvent e) {}
}
