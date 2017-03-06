package de.thecode.android.tazreader.acra;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import de.thecode.android.tazreader.okhttp3.OkHttp3Helper;

import org.acra.ACRAConstants;
import org.acra.ReportField;
import org.acra.collector.CrashReportData;
import org.acra.config.ACRAConfiguration;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;
import org.acra.sender.ReportSenderFactory;
import org.acra.util.HttpRequest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Mate on 05.03.2017.
 */

public class Okhttp3Sender implements ReportSender {


    private final ACRAConfiguration config;
    private final OkHttpClient      client;

    private Okhttp3Sender(@NonNull ACRAConfiguration config, @NonNull OkHttpClient client) {
        this.config = config;
        this.client = client;
    }

    @Override
    public void send(@NonNull Context context, @NonNull CrashReportData report) throws ReportSenderException {

        String versionCode = report.getProperty(ReportField.APP_VERSION_CODE);
        report.putNumber(ReportField.APP_VERSION_CODE,Integer.valueOf(versionCode.substring(1,versionCode.length())));

        try {
            HttpUrl url = HttpUrl.parse(config.formUri());


            Request.Builder requestBuilder = new Request.Builder();

            // Generate report body depending on requested type
            final String reportAsString;
            switch (config.reportType()) {
                case JSON:
                    reportAsString = report.toJSON()
                                           .toString();
                    break;
                case FORM:
                default:
                    reportAsString = HttpRequest.getParamsAsFormString(report);
                    break;
            }

            RequestBody body = RequestBody.create(MediaType.parse(config.reportType()
                                                                        .getContentType() + "; charset=utf-8"), reportAsString);

            switch (config.httpMethod()) {
                case POST:
                    requestBuilder.post(body);
                    break;
                case PUT:
                    url = url.newBuilder()
                             .addEncodedPathSegment(report.getProperty(ReportField.REPORT_ID))
                             .build();
                    requestBuilder.put(body);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown method: " + config.httpMethod());
            }

            Request request = requestBuilder.url(url)
                                            .build();

            Response response = client.newCall(request)
                                      .execute();

            if (!response.isSuccessful()) {
                throw new IOException(response.code() + " " + response.body()
                                                                      .string());
            }

        } catch (IOException e) {
            throw new ReportSenderException(
                    "Error while sending " + config.reportType() + " report via Http " + config.httpMethod(), e);
        }
    }


    public static class Factory implements ReportSenderFactory {
        @NonNull
        @Override
        public ReportSender create(@NonNull Context context, @NonNull ACRAConfiguration config) {

            final String login = isNull(config.formUriBasicAuthLogin()) ? null : config.formUriBasicAuthLogin();
            final String password = isNull(config.formUriBasicAuthPassword()) ? null : config.formUriBasicAuthPassword();
            OkHttpClient.Builder clientBuilder = OkHttp3Helper.getInstance(context)
                                                              .getOkHttpClientBuilder(login, password);
            clientBuilder.connectTimeout(config.connectionTimeout(), TimeUnit.MILLISECONDS)
                         .readTimeout(config.socketTimeout(), TimeUnit.MILLISECONDS)
                         .writeTimeout(config.socketTimeout(), TimeUnit.MILLISECONDS);


            return new Okhttp3Sender(config, clientBuilder.build());
        }

        private boolean isNull(@Nullable String aString) {
            return aString == null || ACRAConstants.NULL_VALUE.equals(aString);
        }
    }


}
