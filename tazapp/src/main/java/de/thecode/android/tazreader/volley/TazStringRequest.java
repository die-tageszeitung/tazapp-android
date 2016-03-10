package de.thecode.android.tazreader.volley;

import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;

import java.io.UnsupportedEncodingException;


public class TazStringRequest extends StringRequest {
    
    public interface MyStringErrorListener  {
        public void onErrorResponse(VolleyError error,String string);
    }
    
    
    private final MyStringErrorListener mErrorListener;
    
    

    public TazStringRequest(int method, String url, Listener<String> listener, MyStringErrorListener errorListener) {
        super(method, url, listener, null);
        mErrorListener = errorListener;
    }
    
    
    @Override
    public void deliverError(VolleyError error) {
        
        if (mErrorListener != null) {
            
            String parsed;
            try {
                parsed = new String(error.networkResponse.data, HttpHeaderParser.parseCharset(error.networkResponse.headers));
            } catch (UnsupportedEncodingException e) {
                parsed = new String(error.networkResponse.data);
            } catch (NullPointerException e)
            {
                parsed = null;
            }
            mErrorListener.onErrorResponse(error,parsed);
        }
    }
    

}
