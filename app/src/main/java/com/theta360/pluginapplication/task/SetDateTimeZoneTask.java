/**
 * Copyright 2018 Ricoh Company, Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.theta360.pluginapplication.task;

import android.os.AsyncTask;
import android.util.Log;

import com.theta360.pluginapplication.network.HttpConnector;


public class SetDateTimeZoneTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "SetDateTimeZone";

    private String setDateTimeZone;

    public SetDateTimeZoneTask(String inDateTimeZone) {
        this.setDateTimeZone = inDateTimeZone;
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    synchronized protected String doInBackground(Void... params) {
        HttpConnector camera = new HttpConnector("127.0.0.1:8080");
        String strResult = "";

        Log.d(TAG, "Pre Set DateTimeZone: Val=" + setDateTimeZone );
        String strJsonSetDateTimeZone = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{\"dateTimeZone\":\"" + setDateTimeZone + "\"} } }";
        strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonSetDateTimeZone);
        Log.d(TAG, "Aft Set DateTimeZone=" + strResult);

        //結果が"Command executed is currently disabled."
        //のときは、日時設定->自動 がオフの時だが、
        //プラグイン起動時にチェック済のため、ここでは対応不要

        return setDateTimeZone;
    }

    @Override
    protected void onPostExecute(String result) {
        //無処理
    }
}
