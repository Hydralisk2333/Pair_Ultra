package com.example.pair_ultra;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static void checkPermissions(Activity activity, String[] permissions) {
        // Marshmallow开始才用申请运行时权限
        int MY_PERMISSIONS_REQUEST = 1001;
        List<String> mPermissionList = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < permissions.length; i++) {
                if (ContextCompat.checkSelfPermission(activity, permissions[i]) !=
                        PackageManager.PERMISSION_GRANTED) {
                    mPermissionList.add(permissions[i]);
                }
            }
            if (!mPermissionList.isEmpty()) {
                String[] needPermissions = mPermissionList.toArray(new String[mPermissionList.size()]);
                ActivityCompat.requestPermissions(activity, needPermissions, MY_PERMISSIONS_REQUEST);
            }
            Log.d("need permit num", mPermissionList.size()+"");
        }
    }

    public static ArrayList<String> readFromFile(Context context, int fileId) throws IOException {

        ArrayList<String> strArray = new ArrayList<String>();
        InputStream fileInputStream = context.getResources().openRawResource(fileId);
        InputStreamReader reader = new InputStreamReader(fileInputStream);
        BufferedReader br = new BufferedReader(reader);
        String line = "";
        line = br.readLine();
        while(line != null && !line.equals("")) {
            strArray.add(line);
            line = br.readLine();
        }
//        for(int i=0;i<strArray.size();i++) {
//            System.out.println(strArray.get(i));
//        }
        return strArray;
    }

}
