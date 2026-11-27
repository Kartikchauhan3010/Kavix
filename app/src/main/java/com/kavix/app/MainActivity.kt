package com.kavix.app
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*
class MainActivity:Activity(){
 private lateinit var st:TextView;private lateinit var out:TextView;private lateinit var hist:TextView
 private fun dp(i:Int)=(i*resources.displayMetrics.density).toInt()
 override fun onCreate(b:Bundle?){
  super.onCreate(b)
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(28),dp(16),dp(16))}
  fun tv(t:String,sz:Float=15f)=TextView(this).apply{text=t;textSize=sz;setPadding(0,dp(6),0,dp(6))}
  fun btn(t:String,f:()->Unit)=Button(this).apply{text=t;setOnClickListener{f()}}
  st=tv("");out=tv("");hist=tv("")
  val box=EditText(this).apply{hint="Paste any message or email here to check it";minLines=3;gravity=android.view.Gravity.TOP}
  root.addView(tv("🛡️ Kavix",28f))
  root.addView(tv("Checks every SMS, WhatsApp and email alert on your phone, and tells you what to do. All checking happens on your device."))
  root.addView(st)
  root.addView(btn("1. Allow SMS scanning + alerts"){
   val p=arrayListOf(Manifest.permission.RECEIVE_SMS);if(Build.VERSION.SDK_INT>=33)p.add(Manifest.permission.POST_NOTIFICATIONS)
   requestPermissions(p.toTypedArray(),1)})
  root.addView(btn("2. Allow WhatsApp + email scanning"){startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))})
  root.addView(tv("Manual check",18f));root.addView(box)
  root.addView(btn("Check now"){val v=Engine.analyze(box.text.toString(),"","Manual")
   val e=listOf("🟢 Looks safe","🟡 Be careful","🔴 Likely scam")[v.level]
   out.text=e+"  (${v.score}/100)\n"+(if(v.level>0)(Engine.ADV[v.top]?:"")+"\n\n" else "")+v.hits.joinToString("\n"){"• ${it.l} (${if(it.p>0)"+" else ""}${it.p})"}})
  root.addView(out)
  root.addView(btn("📞 Call 1930 (cyber fraud helpline)"){startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:1930")))})
  root.addView(btn("Report on cybercrime.gov.in"){startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://cybercrime.gov.in")))})
  root.addView(tv("Recent alerts",18f));root.addView(hist)
  setContentView(ScrollView(this).apply{addView(root)})}
 override fun onResume(){super.onResume();refresh()}
 override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){refresh()}
 private fun refresh(){
  val sms=checkSelfPermission(Manifest.permission.RECEIVE_SMS)==PackageManager.PERMISSION_GRANTED
  val nt=Build.VERSION.SDK_INT<33||checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
  val ls=Settings.Secure.getString(contentResolver,"enabled_notification_listeners")?.contains(packageName)==true
  fun f(b:Boolean)=if(b)"ON ✅" else "OFF ❌"
  st.text="SMS scanning: ${f(sms)}\nAlert pop-ups: ${f(nt)}\nWhatsApp/email scanning: ${f(ls)}"
  val a=try{JSONArray(getSharedPreferences("kx",0).getString("h","[]"))}catch(e:Exception){JSONArray()}
  val df=SimpleDateFormat("dd MMM HH:mm",Locale.getDefault())
  hist.text=if(a.length()==0)"Nothing yet. Alerts appear here." else (0 until a.length()).joinToString("\n\n"){val o=a.getJSONObject(it)
   listOf("🟢","🟡","🔴")[o.getInt("l")]+" "+o.getString("s")+" · "+o.getString("f")+" · "+df.format(Date(o.getLong("t")))+"\n"+o.getInt("p")+"/100 "+o.getString("w")}}
}
