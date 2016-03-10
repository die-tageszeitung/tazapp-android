package de.thecode.android.tazreader.volley;


import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thecode.android.tazreader.data.Paper;

public class TazKioskRequest extends Request<Paper> {

    private static final Logger log = LoggerFactory.getLogger(TazKioskRequest.class);
    
    private final Listener<Paper> mListener;
    private final String mUrl;

    public TazKioskRequest(int method, String url, Listener<Paper> listener, ErrorListener errorlistener) {
        super(method, url, errorlistener);
        mListener = listener;
        mUrl = url;
    }

    @Override
    protected Response<Paper> parseNetworkResponse(NetworkResponse response) {
        
        String contenttype =  response.headers.get("Content-Type");
        if ("application/taz+android+app".equals(contenttype)) {
            String disposition = response.headers.get("Content-Disposition");
            log.info( "Content-disposition: {}",disposition);
            String filename = disposition.substring(disposition.lastIndexOf('=') + 1);
            
            Paper kioskPaper = new Paper();
            //kioskPaper.setDefaultFlags();
            kioskPaper.setKiosk(true);
            kioskPaper.setLink(mUrl);
            kioskPaper.setTitle(filename);
            return Response.success(kioskPaper, HttpHeaderParser.parseCacheHeaders(response));
        }
        return Response.error(new VolleyError("Fehler beim parsen der Antwort"));
    }

    @Override
    protected void deliverResponse(Paper response) {
        mListener.onResponse(response);
    }



}
