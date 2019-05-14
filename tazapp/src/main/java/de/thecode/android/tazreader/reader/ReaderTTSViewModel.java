package de.thecode.android.tazreader.reader;

import android.app.Application;
import android.media.AudioManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import de.thecode.android.tazreader.utils.SingleLiveEvent;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import timber.log.Timber;

public class ReaderTTSViewModel extends AndroidViewModel implements TextToSpeech.OnInitListener{

    public enum TTS {DISABLED, INIT, IDLE, PLAYING, PAUSED}

    public enum TTSERROR {UNKNOWN, LANG_MISSING_DATA, LANG_NOT_SUPPORTED}

    private static final String TTSBREAK = "##TTSBREAK";

    private TextToSpeech tts;
    private TTS ttsState = TTS.DISABLED;
    private String utteranceBaseId;
    private ArrayList<String> sentencesOrder = new ArrayList<>();
    private ArrayList<String> sentencesOrderOriginal;
    private HashMap<String, String> sentences = new HashMap<>();

    private SingleLiveEvent<TTS> liveTtsState = new SingleLiveEvent<>();
    private SingleLiveEvent<TTSERROR> liveTtsError = new SingleLiveEvent<>();


    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = focusChange -> {
        Timber.d("focusChange: %s", focusChange);
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            if (getTtsState() == TTS.PLAYING) pauseTts();
        }
    };

    public ReaderTTSViewModel(@NonNull Application application) {
        super(application);
    }

    public SingleLiveEvent<TTS> getLiveTtsState() {
        return liveTtsState;
    }

    public SingleLiveEvent<TTSERROR> getLiveTtsError() {
        return liveTtsError;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.GERMAN);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                setTtsState(TTS.DISABLED);
                if (result == TextToSpeech.LANG_MISSING_DATA) liveTtsError.setValue(TTSERROR.LANG_MISSING_DATA);
                else if (result == TextToSpeech.LANG_NOT_SUPPORTED) liveTtsError.setValue(TTSERROR.LANG_NOT_SUPPORTED);
                tts.shutdown();
            } else {
                tts.setOnUtteranceProgressListener(ttsListener);
                if (getTtsState() == TTS.INIT) {
                    startTts();
                } else {
                    setTtsState(TTS.IDLE);
                }
            }
        } else {
            setTtsState(TTS.DISABLED);
            liveTtsError.setValue(TTSERROR.UNKNOWN);
        }
    }

    public void initTts(String utteranceBaseId, CharSequence text) {
        if (getTtsState() == TTS.DISABLED) {
            setTtsState(TTS.INIT);
            prepareTts(utteranceBaseId, text);
            tts = new TextToSpeech(getApplication(), this);
        }
    }

    private void setTtsState(@NonNull TTS newState) {
        TTS oldState = ttsState;
        ttsState = newState;
        if (!newState.equals(oldState)) {
            liveTtsState.postValue(ttsState);
        }
    }

    public TTS getTtsState() {
        return ttsState;
    }

    public String getUtteranceBaseId() {
        return utteranceBaseId;
    }


    public void prepareTts(String utteranceBaseId, CharSequence text) {

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
                        utteranceId = utteranceBaseId + counter;
                        sentencesOrder.add(utteranceId);
                        sentences.put(utteranceId, makeSilenceTag(200));
                        counter++;
                    }
                } else {
                    String utteranceId = utteranceBaseId + counter;
                    sentencesOrder.add(utteranceId);
                    sentences.put(utteranceId, makeSilenceTag(700));
                    counter++;
                }
            }
        }

        sentencesOrderOriginal = new ArrayList<>(sentencesOrder);
    }

    private String makeSilenceTag(long millis) {
        return TTSBREAK + millis;
    }

    public void startTts() {
        Pattern p = Pattern.compile("^" + TTSBREAK + "(\\d+)$");
        for (String utteranceId : sentencesOrder) {
            String sentence = sentences.get(utteranceId);
            if (sentence.length() > 0) {
                Matcher m = p.matcher(sentence);
                if (m.find()) {
                    long value = Long.valueOf(m.group(1));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        tts.playSilentUtterance(value, TextToSpeech.QUEUE_ADD, utteranceId);
                    } else {
                        HashMap<String, String> map = new HashMap<>();
                        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
                        tts.playSilence(value, TextToSpeech.QUEUE_ADD, map);
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        tts.speak(sentence, TextToSpeech.QUEUE_ADD, null, utteranceId);
                    } else {
                        HashMap<String, String> map = new HashMap<>();
                        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
                        tts.speak(sentence, TextToSpeech.QUEUE_ADD, map);
                    }
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

        //if (hasCallback()) getCallback().onTtsStopped();
    }

    public void stopTts() {
        if (tts != null && tts.isSpeaking()) tts.stop();
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
            Timber.d("utteranceId: %s", utteranceId);
            setTtsState(TTS.PLAYING);
        }

        @Override
        public void onDone(String utteranceId) {
            Timber.d("utteranceId: %s", utteranceId);
            done(utteranceId);
        }

        @Override
        public void onError(String utteranceId) {
            Timber.d("utteranceId: %s", utteranceId);

        }

        @Override
        public void onError(String utteranceId, int errorCode) {
            Timber.d("utteranceId: %s, errorCode: %s", utteranceId, errorCode);

        }

        private void done(String utteranceId) {
            if (getTtsState() == TTS.PLAYING) sentencesOrder.remove(utteranceId);
            if (sentencesOrder.size() == 0) {
                setTtsState(TTS.IDLE);
                //if (hasCallback()) getCallback().onTtsStopped();
            }
        }
    };


    public AudioManager.OnAudioFocusChangeListener getAudioFocusChangeListener() {
        return audioFocusChangeListener;
    }

}
