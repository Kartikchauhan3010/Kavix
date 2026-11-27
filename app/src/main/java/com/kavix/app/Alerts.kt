package com.kavix.app
import android.app.*
import android.content.*
import android.graphics.drawable.Icon
import android.net.Uri
import android.provider.Telephony
import android.service.notification.*
import org.json.*
object Alerts{
 const val CH="kavix_alerts"
 private var lastKey="";private var lastAt=0L
 fun check(c:Context,src:String,from:String,text:String){
  val now=System.currentTimeMillis();val key=src+from+text.hashCode()
  if(key==lastKey&&now-lastAt<8000)return
  lastKey=key;lastAt=now
  val v=Engine.analyze(text,from,src)
  val why=v.hits.filter{it.p>0}.take(3).joinToString("; "){it.l}
  save(c,src,from,v,why)
  if(v.level==0)return
  val nm=c.getSystemService(NotificationManager::class.java)
  nm.createNotificationChannel(NotificationChannel(CH,"Scam alerts",NotificationManager.IMPORTANCE_HIGH))
  val f=PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
  val open=PendingIntent.getActivity(c,0,Intent(c,MainActivity::class.java),f)
  val dial=PendingIntent.getActivity(c,1,Intent(Intent.ACTION_DIAL,Uri.parse("tel:1930")),f)
  val title=(if(v.level==2)"🔴 Likely scam" else "🟡 Be careful")+" · $src from ${from.ifBlank{"unknown"}}"
  val body=Engine.ADV[v.top]?:Engine.ADV["gen"]!!
  val n=Notification.Builder(c,CH).setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle(title)
   .setContentText(body).setStyle(Notification.BigTextStyle().bigText("$why\n\n$body"))
   .setContentIntent(open).setAutoCancel(true)
   .addAction(Notification.Action.Builder(Icon.createWithResource(c,android.R.drawable.sym_action_call),"Call 1930",dial).build()).build()
  nm.notify(now.toInt(),n)}
 private fun save(c:Context,src:String,from:String,v:Verdict,why:String){
  val sp=c.getSharedPreferences("kx",0);val a=try{JSONArray(sp.getString("h","[]"))}catch(e:Exception){JSONArray()}
  val o=JSONObject().put("t",System.currentTimeMillis()).put("s",src).put("f",from).put("l",v.level).put("p",v.score).put("w",why)
  val out=JSONArray().put(o);for(i in 0 until minOf(a.length(),29))out.put(a.get(i))
  sp.edit().putString("h",out.toString()).apply()}
}
class SmsReceiver:BroadcastReceiver(){
 override fun onReceive(c:Context,i:Intent){
  if(i.action!=Telephony.Sms.Intents.SMS_RECEIVED_ACTION)return
  val m=Telephony.Sms.Intents.getMessagesFromIntent(i)?:return
  if(m.isEmpty())return
  Alerts.check(c,"SMS",m[0].originatingAddress?:"",m.joinToString(""){it.messageBody?:""})}
}
class NotifListener:NotificationListenerService(){
 private val pk=setOf("com.whatsapp","com.whatsapp.w4b","com.google.android.gm","com.microsoft.office.outlook","com.yahoo.mobile.client.android.mail")
 override fun onNotificationPosted(s:StatusBarNotification){
  if(s.packageName !in pk)return
  if(s.notification.flags and Notification.FLAG_GROUP_SUMMARY!=0)return
  val e=s.notification.extras
  val title=e.getCharSequence(Notification.EXTRA_TITLE)?.toString()?:""
  val text=(e.getCharSequence(Notification.EXTRA_BIG_TEXT)?:e.getCharSequence(Notification.EXTRA_TEXT))?.toString()?:return
  Alerts.check(this,if(s.packageName.contains("whatsapp"))"WhatsApp" else "Email",title,title+"\n"+text)}
}
