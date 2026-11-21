package com.storeworkassistant;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.*;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private LinearLayout list, root;
    private TextToSpeech tts;
    private boolean ttsReady=false;
    private int interval=30;
    private final String[] tasks={
        "Complete pending GRN","Check stock daily","Check previous day's orders",
        "Check scrap records","Match ERP material with shop-floor material",
        "Check store orders and cleanliness","Keep all materials organized",
        "Check all required material and ink stock","Item Issue work (Saturday)",
        "Alternate Day material check"
    };
    private final boolean[] done=new boolean[tasks.length];
    private final ArrayList<CheckBox> checks=new ArrayList<>();

    @Override public void onCreate(Bundle b){ super.onCreate(b); tts=new TextToSpeech(this,this); buildUI(); createChannel(); askNotificationPermission(); scheduleReminder(); }
    @Override public void onInit(int status){ if(status==TextToSpeech.SUCCESS){ ttsReady=true; tts.setLanguage(Locale.US); } }
    private int dp(int n){ return (int)(n*getResources().getDisplayMetrics().density); }
    private TextView text(String s,int size,int color){ TextView v=new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(color); return v; }
    private void buildUI(){
        ScrollView scroll=new ScrollView(this); root=new LinearLayout(this); root.setOrientation(1); root.setPadding(dp(18),dp(22),dp(18),dp(24)); root.setBackgroundColor(Color.rgb(245,248,246)); scroll.addView(root); setContentView(scroll);
        TextView title=text("Store Work Assistant",26,Color.rgb(16,76,62)); title.setTypeface(null,1); root.addView(title);
        TextView sub=text("Daily checklist • Voice reminders",14,Color.DKGRAY); root.addView(sub);
        TextView progress=text("0 of "+tasks.length+" completed",16,Color.rgb(23,107,87)); progress.setTag("progress"); progress.setPadding(0,dp(18),0,dp(12)); root.addView(progress);
        list=new LinearLayout(this); list.setOrientation(1); root.addView(list);
        for(int i=0;i<tasks.length;i++){ final int index=i; CheckBox cb=new CheckBox(this); cb.setText(tasks[i]); cb.setTextSize(16); cb.setPadding(dp(8),dp(10),dp(8),dp(10)); cb.setBackgroundColor(Color.WHITE); cb.setOnCheckedChangeListener((v,checked)->{done[index]=checked; updateProgress();}); checks.add(cb); list.addView(cb,new LinearLayout.LayoutParams(-1,-2)); View gap=new View(this); list.addView(gap,new LinearLayout.LayoutParams(1,dp(7))); }
        TextView intervalLabel=text("Reminder interval",17,Color.DKGRAY); intervalLabel.setTypeface(null,1); intervalLabel.setPadding(0,dp(18),0,dp(6)); root.addView(intervalLabel);
        Spinner spinner=new Spinner(this); String[] choices={"15 minutes","30 minutes","45 minutes","60 minutes","90 minutes","120 minutes"}; spinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,choices)); spinner.setSelection(1); root.addView(spinner); spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){int[] mins={15,30,45,60,90,120}; interval=mins[pos]; scheduleReminder();}});
        Button speak=new Button(this); speak.setText("Speak pending work now"); root.addView(speak); speak.setOnClickListener(v->speakPending());
        Button reset=new Button(this); reset.setText("Reset today's checklist"); root.addView(reset); reset.setOnClickListener(v->{for(int i=0;i<checks.size();i++) checks.get(i).setChecked(false);});
        TextView note=text("Note: 30-minute background reminders depend on Android battery and notification settings. Voice is spoken while the app is open; background reminders appear as notifications.",12,Color.GRAY); note.setPadding(0,dp(16),0,0); root.addView(note);
    }
    private void updateProgress(){int n=0; for(boolean b:done) if(b)n++; TextView p=(TextView)root.findViewWithTag("progress"); if(p!=null)p.setText(n+" of "+tasks.length+" completed");}
    private String pendingText(){StringBuilder s=new StringBuilder("Store work reminder. Pending tasks are: "); int n=0; for(int i=0;i<tasks.length;i++) if(!done[i]){s.append(tasks[i]).append(". ");n++;} if(n==0)return "Great work. All listed tasks are completed."; return s.toString();}
    private void speakPending(){if(ttsReady)tts.speak(pendingText(),TextToSpeech.QUEUE_FLUSH,null,"pending"); else Toast.makeText(this,"Voice engine is starting. Try again shortly.",Toast.LENGTH_SHORT).show();}
    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel("store_work","Store work reminders",NotificationManager.IMPORTANCE_DEFAULT); getSystemService(NotificationManager.class).createNotificationChannel(c);}}
    private void askNotificationPermission(){if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.POST_NOTIFICATIONS},7);}
    private void scheduleReminder(){PeriodicWorkRequest req=new PeriodicWorkRequest.Builder(ReminderWorker.class,interval,TimeUnit.MINUTES).build(); WorkManager.getInstance(this).enqueueUniquePeriodicWork("store_work_reminder",ExistingPeriodicWorkPolicy.UPDATE,req);}
    @Override protected void onDestroy(){if(tts!=null){tts.stop();tts.shutdown();} super.onDestroy();}

    public static class ReminderWorker extends Worker {
        public ReminderWorker(Context c,WorkerParameters p){super(c,p);}
        @Override public Result doWork(){Context c=getApplicationContext(); Intent i=new Intent(c,MainActivity.class); PendingIntent pi=PendingIntent.getActivity(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE); Notification n=new NotificationCompat.Builder(c,"store_work").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Store Work Assistant").setContentText("Please check your pending store tasks.").setContentIntent(pi).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT).build(); NotificationManagerCompatShim.show(c,n); return Result.success();}
    }
    static class NotificationManagerCompatShim { static void show(Context c,Notification n){if(Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(c,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED) ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(1001,n);} }
}
