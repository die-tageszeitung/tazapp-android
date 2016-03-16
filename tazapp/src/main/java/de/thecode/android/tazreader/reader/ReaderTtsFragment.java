package de.thecode.android.tazreader.reader;


import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import de.thecode.android.tazreader.data.TazSettings;

/**
 * Created by mate on 15.03.2016.
 */
public class ReaderTtsFragment extends Fragment implements TextToSpeech.OnInitListener {

    private static final Logger log = LoggerFactory.getLogger(ReaderTtsFragment.class);

    private static final String TAG = "RetainTtsFragment";

    public enum TTS {DISABLED, IDLE, PLAYING, PAUSED}

    private TextToSpeech tts;
    private TTS ttsState = TTS.DISABLED;
    private String utteranceBaseId;
    private ArrayList<String> sentencesOrder = new ArrayList<>();
    private ArrayList<String> sentencesOrderOriginal;
    private HashMap<String, String> sentences = new HashMap<>();

    private WeakReference<ReaderTtsFragmentCallback> callback;


    public static ReaderTtsFragment createOrRetainFragment(FragmentManager fm, ReaderTtsFragmentCallback callback) {
        ReaderTtsFragment fragment = (ReaderTtsFragment) fm.findFragmentByTag(TAG);
        if (fragment == null) {
            fragment = new ReaderTtsFragment();
            fm.beginTransaction()
              .add(fragment, TAG)
              .commit();
        }
        fragment.setCallback(callback);
        return fragment;
    }

    public ReaderTtsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    public void setCallback(ReaderTtsFragmentCallback callback) {
        this.callback = new WeakReference<>(callback);
    }

    private boolean hasCallback() {
        return callback != null && callback.get() != null;
    }

    private ReaderTtsFragmentCallback getCallback() {
        return callback.get();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.GERMAN);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                setTtsState(TTS.DISABLED);
                if (hasCallback()) {
                    getCallback().onTtsInitError(result);
                }
                tts.shutdown();
            } else {
                tts.setOnUtteranceProgressListener(ttsListener);
                setTtsState(TTS.IDLE);
            }
        } else {
            setTtsState(TTS.DISABLED);
            if (hasCallback()) {
                getCallback().onTtsInitError(0);
            }
        }
    }

    public void initTts(Context context) {
        if (getTtsState() == TTS.DISABLED && TazSettings.getPrefBoolean(context, TazSettings.PREFKEY.TEXTTOSPEACH, false))
            tts = new TextToSpeech(context.getApplicationContext(), this);
    }

    private void setTtsState(@NonNull TTS newState) {
        TTS oldState = ttsState;
        ttsState = newState;
        if (!newState.equals(oldState)) {
            if (hasCallback()) getCallback().onTtsStateChanged(ttsState);
        }
    }

    public TTS getTtsState() {
        return ttsState;
    }

    public String getUtteranceBaseId() {
        return utteranceBaseId;
    }


    public void prepareTts(String utteranceBaseId, CharSequence text) {
        flushTts();

        this.utteranceBaseId = utteranceBaseId;

        int maxLength = 2000;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            maxLength = TextToSpeech.getMaxSpeechInputLength();
        }

        String[] paragraphArray = text.toString()
                                      .split("\\n");
        int counter = 0;

        if (paragraphArray.length > 0) {
            for (int i = 0; i < paragraphArray.length; i++) {
                if (paragraphArray[i].length() > 0) {
                    BreakIterator breakIterator = BreakIterator.getSentenceInstance(Locale.GERMANY);
                    breakIterator.setText(paragraphArray[i]);
                    int start = breakIterator.first();

                    for (int end = breakIterator.next(); end != BreakIterator.DONE; start = end, end = breakIterator.next()) {
                        String utteranceId = utteranceBaseId + counter;
                        sentencesOrder.add(utteranceId);
                        sentences.put(utteranceId, paragraphArray[i].substring(start, end));
                        counter++;
                    }
                } else {
                    String utteranceId = utteranceBaseId + counter;
                    sentencesOrder.add(utteranceId);
                    sentences.put(utteranceId, "");
                    counter++;
                }
            }
        }

        sentencesOrderOriginal = new ArrayList<>(sentencesOrder);
    }

    public void startTts() {
        for (String utteranceId : sentencesOrder) {
            String sentence = sentences.get(utteranceId);
            if (sentence.length() > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.speak(sentence, TextToSpeech.QUEUE_ADD, null, utteranceId);

                } else {
                    HashMap<String, String> map = new HashMap<>();
                    map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
                    tts.speak(sentence, TextToSpeech.QUEUE_ADD, map);
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.playSilentUtterance(700, TextToSpeech.QUEUE_ADD, utteranceId);
                } else {
                    HashMap<String, String> map = new HashMap<>();
                    map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
                    tts.playSilence(700, TextToSpeech.QUEUE_ADD, map);
                }
            }
        }

    }

    public void pauseTts() {
        tts.stop();
        if (sentencesOrder.size() > 0) {
            setTtsState(TTS.PAUSED);
        } else {
            setTtsState(TTS.IDLE);
        }
        if (hasCallback()) getCallback().onTtsStopped();
    }

    public void stopTts() {
        if (tts.isSpeaking()) tts.stop();
        setTtsState(TTS.IDLE);
    }

    public void flushTts() {
        stopTts();
        utteranceBaseId = null;
        sentences.clear();
        sentencesOrder.clear();
    }

    public void restartTts() {
        sentencesOrder = new ArrayList<>(sentencesOrderOriginal);
        startTts();
    }


    UtteranceProgressListener ttsListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
            log.debug("utteranceId: {}", utteranceId);
            setTtsState(TTS.PLAYING);
        }

        @Override
        public void onDone(String utteranceId) {
            log.debug("utteranceId: {}", utteranceId);
            done(utteranceId);
        }

        @Override
        public void onError(String utteranceId) {
            log.debug("utteranceId: {}", utteranceId);

        }

        @Override
        public void onError(String utteranceId, int errorCode) {
            log.debug("utteranceId: {}, errorCode: {}", utteranceId, errorCode);

        }

        private void done(String utteranceId) {
            if (getTtsState() == TTS.PLAYING) sentencesOrder.remove(utteranceId);
            if (sentencesOrder.size() == 0) {
                setTtsState(TTS.IDLE);
                if (hasCallback()) getCallback().onTtsStopped();
            }
        }
    };

    AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            log.debug("focusChange: {}", focusChange);
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                if (getTtsState() == TTS.PLAYING) pauseTts();
            }
        }
    };

    public AudioManager.OnAudioFocusChangeListener getAudioFocusChangeListener() {
        return audioFocusChangeListener;
    }


    public interface ReaderTtsFragmentCallback {
        void onTtsStateChanged(TTS newState);

        void onTtsInitError(int error);

        //boolean ttsPreparePlayingInActivty();

        void onTtsStopped();


    }
}
