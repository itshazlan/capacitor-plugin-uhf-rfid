package com.nocola.uhf.rfid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.google.gson.Gson;
import com.nocola.uhf.rfid.function.AndroidWakeLock;
import com.nocola.uhf.rfid.function.SPconfig;
import com.nocola.uhf.rfid.function.ScreenListener;
import com.nocola.uhf.rfid.tools.DjxlExcel;
import com.pow.api.cls.RfidPower;
import com.uhf.api.cls.ErrInfo;
import com.uhf.api.cls.ReadExceptionListener;
import com.uhf.api.cls.ReadListener;
import com.uhf.api.cls.Reader;

import java.io.DataOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jxl.write.WriteException;

@CapacitorPlugin(
  name = "RFID",
  permissions = {
    @Permission(strings = {Manifest.permission.WAKE_LOCK}, alias = RFIDPlugin.WAKE_LOCK)
  }
)
public class RFIDPlugin extends Plugin {

  // Permission alias constants
  static final String WAKE_LOCK = "wake_lock";

  private final RFID implementation = new RFID();

  MyApplication myapp;
  boolean isdefaultmaxpow_V = false;
  public static int nowpower;
  Map<String, Reader.TAGINFO> TagsMap = new LinkedHashMap<String, Reader.TAGINFO>();// Orderly (sorted)
  Lock lockobj = new ReentrantLock(), locktagsm = new ReentrantLock(); // Pay attention to this place

  long vstaticstarttick;
  int vmaxstaticcount;
  int Testcount;
  String strlog;
  int totalcount;
  int totalcountlast = 0;
  long statenvtick;
  boolean isrun;
  Thread staticthread;
  int Test_count = 0;
  int allrdcnt, avgcnt;
  long runtime;
  float Vallcount;
  long Valltime;
  int batt_level;

  ScreenListener l;
  AndroidWakeLock Awl;
  private SoundPool soundPool;
  private final Handler handler = new Handler();

  // When RULE is true, you can set the default platform (RULE when is true, that default one PDA platform)
  boolean RULE_NOSELPT = false;
  RfidPower.PDATYPE PT = RfidPower.PDATYPE.NONE;

  int Conidx_sort = 0;
  int Conidx_epcid = 1;
  int Conidx_count = 2;
  int Conidx_ant = 3;
  int Conidx_pro = 4;
  int Conidx_rssi = 5;
  int Conidx_fre = 6;
  int Conidx_rfu = 7;
  int Conidx_tis = 8;
  int Conidx_data = 9;
  int Conidx_u8tid = 10;
  int Conidx_u8bid = 11;

  List<Map<String, ?>> ListMs = new ArrayList<Map<String, ?>>();
  public int Scount = 0;
  List<String> VstaticTags = new ArrayList<String>();
  Map<String, String> h = new HashMap<String, String>();
  String[] pdatypev;
  DjxlExcel dexel;
  Boolean isConnect = false;
  private String currentCallbackID;

  @Override
  public void load() {
    super.load();

    // Setting permission
    String apkRoot = "chmod 777 " + getActivity().getPackageCodePath();
    runRootCommand(apkRoot);
    // Initialize the sound object
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      AudioAttributes audioAttributes = new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build();
      soundPool = new SoundPool.Builder()
        .setAudioAttributes(audioAttributes)
        .build();
      soundPool.load(this.getContext(), R.raw.beep333, 1);
    } else {
      soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
      soundPool.load(this.getContext(), R.raw.beep333, 1);
    }

    // Wake lock
    Awl = new AndroidWakeLock(
      (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE));

    Awl.WakeLock();

    Application app = getActivity().getApplication();
    myapp = (MyApplication) app;
    myapp.Rparams = myapp.new ReaderParams();
    Coname = MyApplication.Coname;

//    mBroadcastReceiver = new MyBroadcastReceiver();
//    IntentFilter intentFilter = new IntentFilter();
//    intentFilter.addAction(BROADCAST_ACTION1);
//    intentFilter.addAction(BROADCAST_ACTION2);
//    intentFilter.addAction(BROADCAST_ACTION3);
//    // intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
//    getActivity().registerReceiver(mBroadcastReceiver, intentFilter);

    for (int i = 0; i < Coname.length; i++)
      h.put(Coname[i], Coname[i]);
    myapp.needreconnect = false;
    l = new ScreenListener(this);

    if (RULE_NOSELPT) {
      myapp.spf = new SPconfig(this);
      myapp.Rpower = new RfidPower(PT, getActivity().getApplicationContext());
      String path = myapp.Rpower.GetDevPath();
      if (myapp.Rpower.PowerUp()) {
        Reader.READER_ERR er = myapp.Mreader.InitReader_Notype(path, 1);
        myapp.antportc = 1;
        myapp.Address = path;
        Reader.HardwareDetails val = myapp.Mreader.new HardwareDetails();
        er = myapp.Mreader.GetHardwareDetails(val);
        myapp.myhd = val;
        if (er == Reader.READER_ERR.MT_OK_ERR) {
          ConnectHandleUI();
        }
      }
    }

    // Screen monitor
    l.begin(new ScreenStateListener() {

      @Override
      public void onScreenOn() {

      }

      @Override
      public void onScreenOff() {
        Log.d("MYINFO", "onScreenoff");

        if (myapp.Mreader != null) {
          myapp.Mreader.CloseReader();
          myapp.needlisen = true;
        }

        if (myapp.Rpower != null) {
          myapp.Rpower.PowerDown();
          myapp.needreconnect = true;
        }
      }
    });

    myapp.Mreader = new Reader();
    myapp.spf = new SPconfig(this);

    pdatypev = RfidPower.getPdaPlat();
    pdatypev[0] = MyApplication.No;
  }

  public class MyBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = "MyBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
      PluginCall call = bridge.getSavedCall(currentCallbackID);

      if (call == null) {
        Log.e("Error", "Callback missing");
        return;
      }

      if (intent.getAction().equals(BROADCAST_ACTION1)) {
        Reader.TAGINFO tfs = myapp.Mreader.new TAGINFO();
        tfs.AntennaID = intent.getByteExtra("ANT", (byte) 1);
        tfs.CRC = intent.getByteArrayExtra("CRC");
        tfs.EmbededData = intent.getByteArrayExtra("EMD");
        tfs.EmbededDatalen = intent.getShortExtra("EML", (short) 0);

        tfs.EpcId = intent.getByteArrayExtra("EPC");
        tfs.Epclen = intent.getShortExtra("EPL", (short) 0);
        tfs.Frequency = intent.getIntExtra("FRQ", 0);
        tfs.PC = intent.getByteArrayExtra("PC");
        tfs.Phase = intent.getIntExtra("PHA", 0);
        tfs.ReadCnt = intent.getIntExtra("RDC", 0);
        tfs.Res = intent.getByteArrayExtra("RES");
        tfs.RSSI = intent.getIntExtra("RSI", 0);
        tfs.TimeStamp = intent.getIntExtra("TSP", 0);

        customTagsBufferResh(call, Reader.bytes_Hexstr(tfs.EpcId), tfs);

        Log.e("totalCount", String.valueOf(totalcount));
        long readtime = System.currentTimeMillis() - statenvtick;
        Log.e("readTime", "rt:" + String.valueOf(readtime));

      } else if (intent.getAction().equals(BROADCAST_ACTION2)) {

        if (isConnect) {
          System.out.println("Power up");
          startScan(call);
        }

      } else if (intent.getAction().equals(BROADCAST_ACTION3)) {

        if (isConnect) {
          StopFunc(true);
//          Log.e("ListMs", String.valueOf(ListMs));
          Gson gson = new Gson();
//          call.resolve(new JSObject().put("data", gson.toJson(ListMs)));
          notifyListeners("rfid", new JSObject().put("data", gson.toJson(ListMs)));
        }

      } else if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {

        JSObject batt = new JSObject();
        batt.put("batteryLevel", intent.getIntExtra("level", 0));
        batt.put("batteryScale", intent.getIntExtra("scale", 0));

      }
    }
  }

  @PluginMethod
  public void echo(PluginCall call) {
    String value = call.getString("value");

    JSObject ret = new JSObject();
    ret.put("value", implementation.echo(value));
    call.resolve(ret);
  }

  @PluginMethod
  public void connect(PluginCall call) {
    String address = call.getString("address");

    if (address == null) {
      call.reject("Address not entered");
      return;
    }

//    if (myapp.Rpower == null) {
//      call.reject("Please choose a platform");
//      return;
//    }

    Intent pintent = new Intent("android.intent.action.SETTINGS_BJ");
    pintent.putExtra("enable", true);
    getActivity().getApplicationContext().sendBroadcast(pintent);

    int portc = 1;

    myapp.dv = new Reader.deviceVersion();
    Reader.GetDeviceVersion(address, myapp.dv);

    long const_v = System.currentTimeMillis();
    Reader.READER_ERR er = myapp.Mreader.InitReader_Notype(address, portc);
    Log.d("MYINFO",
      "connect cost time:"
        + String.valueOf(System.currentTimeMillis()
        - const_v));

//    mBroadcastReceiver = new MyBroadcastReceiver();
//    IntentFilter intentFilter = new IntentFilter();
//    intentFilter.addAction(BROADCAST_ACTION1);
//    intentFilter.addAction(BROADCAST_ACTION2);
//    intentFilter.addAction(BROADCAST_ACTION3);
//    getActivity().registerReceiver(mBroadcastReceiver, intentFilter);

    if (er == Reader.READER_ERR.MT_OK_ERR) {
      myapp.needlisen = true;
      myapp.needreconnect = false;
      myapp.spf.SaveString("PDATYPE", "1");
      myapp.spf.SaveString("ADDRESS", address);
      myapp.spf.SaveString("ANTPORT", "1");

      call.resolve(new JSObject().put("status", "Device successfully connected"));

      Reader.Inv_Potls_ST ipst = myapp.Mreader.new Inv_Potls_ST();
      ipst.potlcnt = 1;
      ipst.potls = new Reader.Inv_Potl[ipst.potlcnt];
      for (int i = 0; i < ipst.potlcnt; i++) {
        Reader.Inv_Potl ipl = myapp.Mreader.new Inv_Potl();
        ipl.weight = 30;
        ipl.potl = Reader.SL_TagProtocol.SL_TAG_PROTOCOL_GEN2;
        ipst.potls[i] = ipl;
      }
      er = myapp.Mreader.ParamSet(
        Reader.Mtr_Param.MTR_PARAM_TAG_INVPOTL, ipst);
      int[] av = new int[1];
      myapp.Mreader.ParamGet(
        Reader.Mtr_Param.MTR_PARAM_READER_AVAILABLE_ANTPORTS, av);
      myapp.antportc = portc;

      ConnectHandleUI();
      myapp.Address = address;
      myapp.haveinit = true;
      isConnect = true;
    } else {
      isConnect = false;
      call.reject("Connection failed");
    }
  }

  @PluginMethod
  public void disconnect(PluginCall call) {

    JSObject ret = new JSObject();

    if (myapp.Mreader != null) {
      myapp.Mreader.CloseReader();
      myapp.needlisen = true;
      myapp.haveinit = false;
      isConnect = false;
      ret.put("status", "Reader successfully disconnected");
      call.resolve(ret);
    } else {
      isConnect = true;
      call.reject("Unable to disconnect reader");
    }

    Intent pintent = new Intent("android.intent.action.SETTINGS_BJ");
    pintent.putExtra("enable", false);
    getActivity().getApplicationContext().sendBroadcast(pintent);
  }

  @PluginMethod
  public void setOption(PluginCall call) {
    // Set antenna power
    int[] rp = new int[16];
    int[] wp = new int[16];
    for (int i = 0; i < 16; i++) {
      rp[i] = Integer.parseInt("3300");
      wp[i] = rp[i];
    }

    Reader.AntPowerConf apcf = myapp.Mreader.new AntPowerConf();
    apcf.antcnt = myapp.antportc;
    int[] rpow = new int[apcf.antcnt];
    int[] wpow = new int[apcf.antcnt];
    for (int i = 0; i < apcf.antcnt; i++) {
      Reader.AntPower jaap = myapp.Mreader.new AntPower();
      jaap.antid = i + 1;
      jaap.readPower = (short) (rp[i]);
      rpow[i] = jaap.readPower;

      jaap.writePower = (short) (wp[i]);
      wpow[i] = jaap.writePower;
      apcf.Powers[i] = jaap;
    }

    try {
      Reader.READER_ERR er = myapp.Mreader.ParamSet(
        Reader.Mtr_Param.MTR_PARAM_RF_ANTPOWER, apcf);
      if (er == Reader.READER_ERR.MT_OK_ERR) {
        myapp.Rparams.rpow = rpow;
        myapp.Rparams.wpow = wpow;
        call.resolve(new JSObject().put("status", MyApplication.Constr_SetOk));
      } else
        call.reject(MyApplication.Constr_SetFaill);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      call.reject(MyApplication.Constr_setcep);
      return;
    }

    // Set mode handheld
    myapp.Rparams.option = 0;
    myapp.issmartmode = false;
    myapp.Rparams.sleep = 0;
    myapp.isquicklymode = true;

    int[] mp = new int[1];
    String msg = "";
    Reader.READER_ERR er = myapp.Mreader.ParamGet(
      Reader.Mtr_Param.MTR_PARAM_RF_MAXPOWER, mp);
    if (er == Reader.READER_ERR.MT_OK_ERR) {

      msg += "PowMax " + String.valueOf((short) mp[0]) + ",";
      apcf = myapp.Mreader.new AntPowerConf();
      apcf.antcnt = myapp.antportc;
      rpow = new int[apcf.antcnt];
      wpow = new int[apcf.antcnt];
      for (int i = 0; i < apcf.antcnt; i++) {
        Reader.AntPower jaap = myapp.Mreader.new AntPower();
        jaap.antid = i + 1;
        jaap.readPower = (short) (mp[0]);
        rpow[i] = jaap.readPower;

        jaap.writePower = (short) (mp[0]);
        wpow[i] = jaap.writePower;
        apcf.Powers[i] = jaap;
      }
      er = myapp.Mreader.ParamSet(
        Reader.Mtr_Param.MTR_PARAM_RF_ANTPOWER, apcf);
      if (er == Reader.READER_ERR.MT_OK_ERR) {
        myapp.Rparams.rpow = rpow;
        myapp.Rparams.wpow = wpow;
      }
    }

    msg += "Session 1,";
    er = myapp.Mreader.ParamSet(
      Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_SESSION, new int[]{1});
    if (er == Reader.READER_ERR.MT_OK_ERR)
      myapp.Rparams.session = 1;

    //if (myapp.myhd.module==Module_Type.MODOULE_SLR1200)
    {
      myapp.Rparams.option |= 0x10;
      msg += "HM";
    }

    call.resolve(new JSObject().put("status", MyApplication.Constr_set));
  }

  @PluginMethod()
  public void startScan(final PluginCall call) {
    try {
      if (myapp.islatetime) {
        Thread.sleep(myapp.latetimems);
      }

      ListMs.add(h);

      // Determine whether to reconnect
      boolean bl = true;
      if (myapp.needreconnect) {
        int c = 0;
        do {
          bl = reconnect();
          if (!bl)
            call.reject("Reconnection failed");
          c++;
          if (c > 0)
            break;
        } while (true);
      }
      if (!bl)
        return;

      // Generate time quantity report
      if (myapp.isReport_rec) {
        dexel = new DjxlExcel("Time quantity report");

        String[] s2 = new String[]{"Testing frequency", "Number of tags", "Number of labels",
          "Number of new additions", "Total", "Time (milliseconds)"};
        List<String> ls = new ArrayList<String>();
        for (int i = 0; i < s2.length; i++)
          ls.add(s2[i]);
        try {
          dexel.CreatereExcelfile(ls);
        } catch (WriteException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }

      // Electricity module report
      if (myapp.isReport_tep) {
        dexel = new DjxlExcel("Battery temperature report");

        String[] s2 = new String[]{"Time", "Temperature", "Battery%"};
        List<String> ls = new ArrayList<String>();
        for (int i = 0; i < s2.length; i++)
          ls.add(s2[i]);
        try {
          String linestr = myapp.myhd.module.toString()
            + "   ";

          int cnp = nowpower;
          int mu = (int) (myapp.stoptimems / 60000);
          linestr += String.valueOf(cnp) + " "
            + String.valueOf(mu);

          dexel.CreatereExcelfile(linestr + "Record every minute", ls);

          mBroadcastReceiver = new MyBroadcastReceiver();
          IntentFilter intentFilter = new IntentFilter();
          intentFilter.addAction(BROADCAST_ACTION1);
          intentFilter.addAction(BROADCAST_ACTION2);
          intentFilter.addAction(BROADCAST_ACTION3);
          intentFilter
            .addAction(Intent.ACTION_BATTERY_CHANGED);
          getActivity().registerReceiver(mBroadcastReceiver, intentFilter);

        } catch (WriteException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }

      // for quickly mode test
      /**
       * isquicklymode 为true 即快速模式,为false 即普通模式 threadmode 0即线程盘点
       * threadmode 1 为标签事件监听模式
       */
      if (myapp.ThreadMODE == 0) {

        if (myapp.issmartmode) {
          if (myapp.needlisen == true) {
            myapp.Mreader.addReadListener(RL);

            myapp.Mreader.addReadExceptionListener(REL);
            myapp.needlisen = false;
          }

          Reader.READER_ERR er = myapp.Mreader.AsyncStartReading_IT_CT(
            myapp.Rparams.uants,
            myapp.Rparams.uants.length,
            myapp.Rparams.option);
          if (er != Reader.READER_ERR.MT_OK_ERR) {
            myapp.Mreader.GetLastDetailError(myapp.ei);
//            call.reject("Failed to start inventory: ");
            return;
          }

        } else {
          if (myapp.isquicklymode) {
            Reader.READER_ERR er = myapp.Mreader.AsyncStartReading(
              myapp.Rparams.uants,
              myapp.Rparams.uants.length,
              myapp.Rparams.option);
            if (er != Reader.READER_ERR.MT_OK_ERR) {
              myapp.Mreader.GetLastDetailError(myapp.ei);
//              call.reject("Failed to start inventory: ");
              return;
            }
          }

//          handler.postDelayed(runnable_MainActivity, 0);
          new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
              String[] tag = null;
              int[] tagcnt = new int[1];
              tagcnt[0] = 0;

              int streadt = 0, enreadt = 0;
              synchronized (this) {
                Reader.READER_ERR er;
                streadt = (int) System.currentTimeMillis();
                if (myapp.isquicklymode) {
                  er = myapp.Mreader.AsyncGetTagCount(tagcnt);
                } else {
                  er = myapp.Mreader.TagInventory_Raw(myapp.Rparams.uants,
                    myapp.Rparams.uants.length,
                    (short) myapp.Rparams.readtime, tagcnt);
                }

                if (er == Reader.READER_ERR.MT_OK_ERR) {
                  if (tagcnt[0] > 0) {
                    playSound();
                    if (myapp.issound)
                      soundPool.play(1, 1, 1, 0, 0, 1);
                    tag = new String[tagcnt[0]];
                    for (int i = 0; i < tagcnt[0]; i++) {
                      Reader.TAGINFO tfs = myapp.Mreader.new TAGINFO();

                      int streadt2 = (int) System.currentTimeMillis();
                      if (myapp.isquicklymode)
                        er = myapp.Mreader.AsyncGetNextTag(tfs);
                      else
//                      Log.e("tfs1", String.valueOf(tfs.Frequency));
                        er = myapp.Mreader.GetNextTag(tfs);
//                      Log.e("tfs2", String.valueOf(tfs.Frequency));
                      int edreadt2 = (int) System.currentTimeMillis();

                      if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET) {
                        myapp.needreconnect = true;
                        StopFunc(true);
                      }

                      // 判断是否需要改写
                      /*
                       * String epc=Reader.bytes_Hexstr(tfs.EpcId);
                       *
                       * if(epc.length()==8&&epc.substring(0,
                       * 4).equals("A000")) { String
                       * newepc="A0000000000000000000"+epc.substring(4);
                       * emdop_rewriteepc(epc, newepc, tfs.AntennaID); }
                       */

                      // Log.d("MYINFO","debug gettag:"+er.toString());
                      // Log.d("MYINFO","debug tag:"+Reader.bytes_Hexstr(tfs.EpcId));

                      if (er == Reader.READER_ERR.MT_OK_ERR) {
                        tag[i] = Reader.bytes_Hexstr(tfs.EpcId);

                        // 刷新标签缓存(fresh tags buffer)
                        customTagsBufferResh(call, tag[i], tfs);

                      } else
                        break; //一旦无法从缓冲区获取标签，就要重新调用读标签方法，不能继续获取标签缓冲
                    }

                    enreadt = (int) System.currentTimeMillis();
                    Log.e("ct:", String.valueOf(enreadt - streadt));
                  }

                } else {
                  myapp.Mreader.GetLastDetailError(myapp.ei);
                  Log.e("error:", String.valueOf(er.value()));

                  if (myapp.isquicklymode && er != Reader.READER_ERR.MT_OK_ERR) {
                    if (er != Reader.READER_ERR.MT_CMD_FAILED_ERR) {
                      StopFunc(true);
                      return;
                    }
                  }

                  if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET) {
                    myapp.needreconnect = true;
                    StopFunc(true);
                  } else
                    handler.postDelayed(this, myapp.Rparams.sleep);
                  return;
                }
              }

              if (tag == null) {
                tag = new String[0];
              }

              long readtime = System.currentTimeMillis() - statenvtick;
              handler.postDelayed(this, myapp.Rparams.sleep);
            }
          }, 0);
        }

      } else if (myapp.ThreadMODE == 1) {
        if (myapp.needlisen == true)
        // 设置盘存到标签时的回调处理函数(set the callback function for
        // inventory tags)
        {
          myapp.Mreader.addReadListener(RL);
          // *
          // 设置读写器发生错误时的回调处理函数(set the callback function for
          // handling error)
          myapp.Mreader.addReadExceptionListener(REL);
          myapp.needlisen = false;
        }

        // 广播形式(forms of broadcasting)
        if (StartReadTags() != Reader.READER_ERR.MT_OK_ERR) {
          call.reject("Failed to start inventory: ");
          return;
        }
      }

      Testcount = 0;
      strlog = "";
      statenvtick = System.currentTimeMillis();
      vstaticstarttick = System.currentTimeMillis() - statenvtick;
      if (myapp.VisStatics) { // vstatichandler.post(runnable_statics);

        totalcount = 0;
        totalcountlast = 0;
        TagsMap.clear();

        ListMs.clear();
        ListMs.add(h);

        isrun = true;
        staticthread = new Thread(runnable_statics_thread);
        staticthread.start();
      }
      myapp.TagsMap.clear();

    } catch (Exception ex) {
      call.reject("Failed to start inventory: ");
    }
  }


  @PluginMethod
  public void stopScan(PluginCall call) {
    StopFunc(true);
    call.resolve(new JSObject().put("status", "Scan stopped"));
  }

  @PluginMethod
  public void clear(PluginCall call) {
    TagsMap.clear();
    myapp.TagsMap.clear();
    ListMs.clear();
    totalcount = 0;
    totalcountlast = 0;
    ListMs.add(h);
    myapp.Curepc = "";
    if (myapp.issmartmode) {
      myapp.Mreader.Reset_IT_CT();
    }
    call.resolve();
  }

  @PluginMethod()
  public void addWatcher(PluginCall call) {
    nullifyPreviousCall();
    call.setKeepAlive(true);
    currentCallbackID = call.getCallbackId();

    if (currentCallbackID != null) {
      mBroadcastReceiver = new MyBroadcastReceiver();
      IntentFilter intentFilter = new IntentFilter();
      intentFilter.addAction(BROADCAST_ACTION1);
      intentFilter.addAction(BROADCAST_ACTION2);
      intentFilter.addAction(BROADCAST_ACTION3);
//        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
      getActivity().registerReceiver(mBroadcastReceiver, intentFilter);
    }
  }

  @PluginMethod
  public void removeWatcher(PluginCall call) {
    nullifyPreviousCall();
    call.setKeepAlive(false);
//    getActivity().unregisterReceiver(mBroadcastReceiver);
    call.resolve();
  }

  private void nullifyPreviousCall() {
    PluginCall savedCall = bridge.getSavedCall(currentCallbackID);
    if (savedCall != null) {
      savedCall = null;
    }
  }

  // ****************************************事件方式读取标签(event way for get tags)
  MyBroadcastReceiver mBroadcastReceiver = new MyBroadcastReceiver();
  public static final String BROADCAST_ACTION1 = "com.nocola.uhf.rfid";
  public static final String BROADCAST_ACTION2 = "com.eastaeon.rfidkey.KEY_DOWN";
  public static final String BROADCAST_ACTION3 = "com.eastaeon.rfidkey.KEY_UP";

  Reader.READER_ERR StartReadTags() {
    // 初始化结BackReadOption(init BackReadOption)

    // 本例只使用天线1进行盘存，如果要使用多个天线则只需要将使用的天线编
    // 号赋值到数组ants中，例如同时使用天线1和2，则代码应该改为ants[0] = 1;
    // ants[1] = 2;antcnt = 2;
    // (the sample only use ant 1 to inventory,if use multiple
    // than use ants arrays with antid, as use ant1 and ant2 at the
    // same,ants[0] = 1;
    // ants[1] = 2;antcnt = 2;)

    /*
     * 是否采用高速模式（目前只有slr11xx和slr12xx系列读写器才支持）,对于
     * 一般标签数量不大，速度不快的应用没有必要使用高速模式,本例没有设置使用高速模式 (whether use high speed
     * mode(only slr11xx and slr12xx series readers supported)，for not many
     * of tags normal，is needn't to use)
     */
    if (myapp.isquicklymode)
      myapp.m_BROption.IsFastRead = true;
    else
      myapp.m_BROption.IsFastRead = false;// 采用非高速模式盘存标签（not use high
    // speed）

    // /非高速盘存模式下必须要设置的参数*******************************************
    // 盘存周期,单位为ms，可根据实际使用的天线个数按照每个天线需要200ms
    // 的方式计算得出,如果启用高速模式则此选项没有任何意义，可以设置为
    // 任意值，或者干脆不设置
    // (in not high speed mode,that must set some params
    // inventory cycle,as ms unit.according to antenna count and each
    // antenna read time as 200ms to
    // get the inventory cycle.is invalid in high speed mode.)
    myapp.m_BROption.ReadDuration = (short) (myapp.Rparams.readtime * myapp.Rparams.uants.length);
    // 盘存周期间的设备不工作时间,单位为ms,一般可设置为0，增加设备不工作
    // 时间有利于节电和减少设备发热（针对某些使用电池供电或空间结构不利
    // 于散热的情况会有帮助）
    // (it is the time that the reader is not working during inventory
    // cycle,as ms unit,generally is set 0
    // increase the sleep time is in favour of save electricity and reduce
    // heating (specially using battery or the space
    // is not good for heat dissipation))
    myapp.m_BROption.ReadInterval = myapp.Rparams.sleep;
    // ****************************************************************************

    // 高速盘存模式参数设置********************************************************
    // (params with high speed mode)
    // 以下为选择使用高速模式才起作用的选项参,照如下设置即可,如果使能高速
    // 模式，即把IsFastRead设置为true则,只需取消以下被注释的代码即可
    /*
     * (the parameters as fallow is avaible in high speed mode,set as
     * fallow. set IsFastRead as true, if enable the high speed mode,and
     * cancel the Following code) // 高速模式下为取得最佳效果设置为0即可(set to 0 to get the
     * best effect in high speed mode)
     */
    if (myapp.m_BROption.IsFastRead) {
      myapp.m_BROption.FastReadDutyRation = 0;

      // 标签信息是否携带识别天线的编号(if with antenna id of tag info)
      myapp.m_BROption.TMFlags.IsAntennaID = true; //

      // 标签信息是否携带标签识别次数(if with read count of tag info)
      myapp.m_BROption.TMFlags.IsReadCnt = false;

      // 标签信息是否携带识别标签时的信号强度 (if with rssi of tag info)
      myapp.m_BROption.TMFlags.IsRSSI = false;

      // 标签信息是否携带时间戳(if with time stamp of tag info)
      myapp.m_BROption.TMFlags.IsTimestamp = false;

      // 标签信息是否携带识别标签时的工作频点(if withfrequency of tag info)
      myapp.m_BROption.TMFlags.IsFrequency = false;

      // 标签信息是否携带识别标签时同时读取的其它bank数据信息,如果要获取在
      // 盘存时同时读取其它bank的信息还必须设置MTR_PARAM_TAG_EMBEDEDDATA参数,
      // （目前只有slr11xx和slr12xx系列读写器才支持）
      // (if with addition data of tag info
      // read the bank data when inventorying,you must set param
      // ofMTR_PARAM_TAG_EMBEDEDDATA
      // (only slr11xx and slr12xx series reader suppoted)
      myapp.m_BROption.TMFlags.IsEmdData = false;

      // 保留字段，可始终设置为0(reserver field always as 0)
      myapp.m_BROption.TMFlags.IsRFU = false;

    }

    return myapp.Mreader.StartReading(myapp.Rparams.uants,
      myapp.Rparams.uants.length, myapp.m_BROption);
  }

  // Label event (tag event)
  ReadListener RL = new ReadListener() {
    @Override
    public void tagRead(Reader r, final Reader.TAGINFO[] tag) {
      // TODO Auto-generated method stub
      Intent intent = new Intent();
      intent.setAction(BROADCAST_ACTION1);
      for (int i = 0; i < tag.length; i++) {
        intent.putExtra("ANT", tag[i].AntennaID);
        intent.putExtra("CRC", tag[i].CRC);
        intent.putExtra("EMD", tag[i].EmbededData);
        intent.putExtra("EML", tag[i].EmbededDatalen);
        intent.putExtra("EPC", tag[i].EpcId);
        intent.putExtra("EPL", tag[i].Epclen);
        intent.putExtra("FRQ", tag[i].Frequency);
        intent.putExtra("PC", tag[i].PC);
        intent.putExtra("PHA", tag[i].Phase);
        intent.putExtra("RDC", tag[i].ReadCnt);
        intent.putExtra("RES", tag[i].Res);
        intent.putExtra("RSI", tag[i].RSSI);
        intent.putExtra("TSP", tag[i].TimeStamp);
        if (myapp.issound)
          soundPool.play(1, 1, 1, 0, 0, 1);
        getActivity().sendBroadcast(intent);
      }
    }
  };

  ReadExceptionListener REL = new ReadExceptionListener() {
    @Override
    public void tagReadException(Reader r, final Reader.READER_ERR re) {
      // TODO Auto-generated method stub
      Message mes = new Message();
      mes.what = 2;
      Bundle bd = new Bundle();
      bd.putString("Msg_error_2", re.toString());
      mes.setData(bd);
      handler2.sendMessage(mes);
    }
  };

  @SuppressLint("HandlerLeak")
  public Handler handler2 = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      Bundle bd = msg.getData();
      switch (msg.what) {
        case 0: {
          handler.removeCallbacks(runnable_MainActivity);
          break;
        }
        case 1: {
          StopFunc(false);
          break;
        }
        case 2: {
          ErrInfo ei = new ErrInfo();
          myapp.Mreader.GetLastDetailError(ei);
          Test_count++;
          break;
        }
        case 3:
        case 4: {
          break;
        }
        case 5: {
          ListMs.clear();
          ListMs.add(h);
          handler.postDelayed(runnable_MainActivity, 0);
          break;
        }
        case 6: {
          ListMs.clear();
          ListMs.add(h);
        }
      }
    }
  };

  private void playSound() {
    Intent pintent = new Intent("com.eastaeon.android.PLAY_SOUND_EFFECT");
    pintent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
    getActivity().sendBroadcast(pintent);
  }

  private final Runnable runnable_MainActivity = new Runnable() {
    public void run() {

      String[] tag = null;
      int[] tagcnt = new int[1];
      tagcnt[0] = 0;

      int streadt = 0, enreadt = 0;
      synchronized (this) {
        Reader.READER_ERR er;
        streadt = (int) System.currentTimeMillis();
        if (myapp.isquicklymode) {
          er = myapp.Mreader.AsyncGetTagCount(tagcnt);
        } else {
          er = myapp.Mreader.TagInventory_Raw(myapp.Rparams.uants,
            myapp.Rparams.uants.length,
            (short) myapp.Rparams.readtime, tagcnt);
        }

        if (er == Reader.READER_ERR.MT_OK_ERR) {
          if (tagcnt[0] > 0) {
            playSound();
            if (myapp.issound)
              soundPool.play(1, 1, 1, 0, 0, 1);
            tag = new String[tagcnt[0]];
            for (int i = 0; i < tagcnt[0]; i++) {
              Reader.TAGINFO tfs = myapp.Mreader.new TAGINFO();

              int streadt2 = (int) System.currentTimeMillis();
              if (myapp.isquicklymode)
                er = myapp.Mreader.AsyncGetNextTag(tfs);
              else
                er = myapp.Mreader.GetNextTag(tfs);
              int edreadt2 = (int) System.currentTimeMillis();

              if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET) {
                myapp.needreconnect = true;
                StopFunc(true);
              }

              // 判断是否需要改写
              /*
               * String epc=Reader.bytes_Hexstr(tfs.EpcId);
               *
               * if(epc.length()==8&&epc.substring(0,
               * 4).equals("A000")) { String
               * newepc="A0000000000000000000"+epc.substring(4);
               * emdop_rewriteepc(epc, newepc, tfs.AntennaID); }
               */

              // Log.d("MYINFO","debug gettag:"+er.toString());
              // Log.d("MYINFO","debug tag:"+Reader.bytes_Hexstr(tfs.EpcId));

              if (er == Reader.READER_ERR.MT_OK_ERR) {
                tag[i] = Reader.bytes_Hexstr(tfs.EpcId);

                // 刷新标签缓存(fresh tags buffer)
                TagsBufferResh(tag[i], tfs);

              } else
                break;//一旦无法从缓冲区获取标签，就要重新调用读标签方法，不能继续获取标签缓冲
            }

            enreadt = (int) System.currentTimeMillis();
            Log.e("ct:", String.valueOf(enreadt - streadt));
          }

        } else {
          myapp.Mreader.GetLastDetailError(myapp.ei);
          Log.e("error:", String.valueOf(er.value()));

          if (myapp.isquicklymode && er != Reader.READER_ERR.MT_OK_ERR) {
            if (er != Reader.READER_ERR.MT_CMD_FAILED_ERR) {

              StopFunc(true);

              return;
            }
          }

          if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET) {

            myapp.needreconnect = true;
            StopFunc(true);

          } else
            handler.postDelayed(this, myapp.Rparams.sleep);
          return;

        }
      }

      if (tag == null) {
        tag = new String[0];
      }

      long readtime = System.currentTimeMillis() - statenvtick;
      handler.postDelayed(this, myapp.Rparams.sleep);
    }
  };

  private Runnable runnable_statics_thread = new Runnable() {
    public void run() {

      while (isrun) {
        // 每秒统计
        // *
        long evetime = System.currentTimeMillis() - statenvtick;

        if (evetime - vstaticstarttick >= 1000) {
          int tagtimes = 0;
          int tagcountunit = 0;
          int tagnew = 0;
          int tagtoal = 0;
          // 时间数量报表
          if (myapp.isReport_rec && evetime < myapp.stoptimems) {
            if (lockobj.tryLock()) {
              try {
                tagcountunit = VstaticTags.size();
                tagtimes = Scount;
                tagtoal = totalcount;

                tagnew = totalcount - totalcountlast;
                totalcountlast = totalcount;

                Scount = 0;
                VstaticTags.clear();
              } catch (Exception ex) {

              } finally {
                lockobj.unlock(); // 释放锁
              }

              try {
                // 导出xls文件
                dexel.WriteExcelfile(new Object[]{
                  (Integer) (Testcount + 1),
                  (Integer) tagtimes,
                  (Integer) tagcountunit,
                  (Integer) tagnew,
                  (Integer) tagtoal,
                  (Long) evetime});

              } catch (Exception ex) {

              }
            }
          }

          // 标签总次数 平均次数
          Iterator<Map.Entry<String, Reader.TAGINFO>> iesb;

          if (locktagsm.tryLock()) {
            iesb = TagsMap.entrySet().iterator();

            while (iesb.hasNext()) {
              Reader.TAGINFO bd = iesb.next().getValue();
              allrdcnt += bd.ReadCnt;
            }
            locktagsm.unlock();
          }

          if (totalcount > 0)
            avgcnt = allrdcnt / totalcount;

          if (tagcountunit > vmaxstaticcount)
            vmaxstaticcount = tagcountunit;
          String text = " a:" + String.valueOf(tagcountunit)
            + "/s m:" + String.valueOf(vmaxstaticcount) + "/s ";

          // tv_statics.setText(text);
          Message msg = new Message();
          msg.what = 4;
          Bundle data = new Bundle();

          data.putString("Msg_sec", String.valueOf(text));
          msg.setData(data);
          // 发送消息到Handler
          handler2.sendMessage(msg);

          avgcnt = allrdcnt = 0;
          vstaticstarttick = evetime;
        }
        // */
        runtime = System.currentTimeMillis() - statenvtick;
        if ((myapp.isstoptime && runtime >= myapp.stoptimems) ||
          (myapp.isstopcount && totalcount >= myapp.stopcount)) {
          Testcount++;
          Message msg_stop = new Message();
          msg_stop.what = 1;
          handler2.sendMessage(msg_stop);

          if (myapp.VisTestFor) {
            if (Testcount <= myapp.Vtestforcount) {

              // 标签总次数 平均次数
              Iterator<Map.Entry<String, Reader.TAGINFO>> iesb;

              if (locktagsm.tryLock()) {
                iesb = TagsMap.entrySet().iterator();
                while (iesb.hasNext()) {
                  Reader.TAGINFO bd = iesb.next().getValue();
                  allrdcnt += bd.ReadCnt;
                }
                locktagsm.unlock();
              }
              if (totalcount > 0)
                avgcnt = allrdcnt / totalcount;

              //dlog.toDlog("static thread sleep");
              try {
                if (isrun)
                  Thread.sleep(myapp.Vtestforsleep);
              } catch (InterruptedException e) {
              }

              if (Testcount < myapp.Vtestforcount) {
                TagsMap.clear();
                totalcount = 0;
                totalcountlast = 0;
              }

              if (isrun) {
                if (myapp.isquicklymode) {
                  //dlog.toDlog("static  quick read again");
                  Reader.READER_ERR er = myapp.Mreader
                    .AsyncStartReading(
                      myapp.Rparams.uants,
                      myapp.Rparams.uants.length,
                      myapp.Rparams.option);
                  if (er != Reader.READER_ERR.MT_OK_ERR) {
                    Message msg_exstop = new Message();
                    msg_exstop.what = 1;
                    handler2.sendMessage(msg_exstop);
                    return;
                  }
                }
                //dlog.toDlog("static thread  again");
                Message msg_start = new Message();
                msg_start.what = 5;
                handler2.sendMessage(msg_start);
                statenvtick = System.currentTimeMillis();
                vstaticstarttick = System.currentTimeMillis() - statenvtick;
              }

            }
          } else
            return;
        } else
          try {
            Thread.sleep(myapp.Rparams.sleep);
          } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
      }
    }
  };

  private void TagsBufferResh(String EPC, Reader.TAGINFO tfs) {

    if (myapp.VisStatics)
      fcastatic(EPC);

    String key = EPC;
    String epcstr = EPC;
    String tid = "", bid = "";
    if (myapp.nxpu8 == 1) {
      key = key.substring(0, key.length() - 4);
      bid = epcstr.substring(epcstr.length() - 4, epcstr.length());
      epcstr = epcstr.substring(0, epcstr.length() - 4);
    } else if (myapp.nxpu8 == 2) {
      key = key.substring(0, key.length() - 24);
      tid = epcstr.substring(epcstr.length() - 24, epcstr.length());
      epcstr = epcstr.substring(0, epcstr.length() - 24);
    } else if (myapp.nxpu8 == 3) {
      key = key.substring(0, key.length() - 4);
      bid = epcstr.substring(epcstr.length() - 4, epcstr.length());
      epcstr = epcstr.substring(0, epcstr.length() - 4);
    }

    if (epcstr.length() < 24)
      epcstr = String.format("%-24s", epcstr);

    String emdstr = "";
    String rfu = "";

    if (tfs.EmbededDatalen > 0) {
      char[] out = new char[tfs.EmbededDatalen * 2];
      myapp.Mreader.Hex2Str(tfs.EmbededData, tfs.EmbededDatalen, out);
      emdstr = String.valueOf(out);
      if (myapp.nxpu8 == 1)
        tid = emdstr;
    }

    if (myapp.nxpu8 != 0) {
      char[] out2 = new char[4];
      myapp.Mreader.Hex2Str(tfs.Res, 2, out2);
      rfu = String.valueOf(out2);
    } else {
      //rfu=String.valueOf(((tfs.Res[0]<<8|tfs.Res[1])&0x3f)*180/64);
      rfu = String.valueOf(tfs.Res[1] & 0x3f);
      //rfu=String.valueOf((tfs.Res[1]&0x3f)*180/64);
    }

    if (myapp.isUniByEmd) {
      if (myapp.nxpu8 == 2) {
        key += tid;// nxpu8==2 epcid本身包含tid(nxpu8==2 epcid contain tid)
      } else {
        if (emdstr.isEmpty())
          return;

        if (myapp.nxpu8 != 2)
          key += emdstr;
      }

    }

    if (myapp.isUniByAnt)
      key += String.valueOf(tfs.AntennaID);

    if (!TagsMap.containsKey(key)) {

      if (locktagsm.tryLock()) {
        TagsMap.put(key, tfs);

        totalcount++;
        locktagsm.unlock();
      }


      // show
      Map<String, String> m = new HashMap<String, String>();
      m.put(Coname[Conidx_sort], String.valueOf(TagsMap.size()));

      if (myapp.isconngoods && myapp.listName.containsKey(epcstr.trim()))
        m.put(Coname[Conidx_epcid], myapp.listName.get(epcstr.trim()));
      else
        m.put(Coname[Conidx_epcid], epcstr);

      m.put(Coname[Conidx_count], String.valueOf(tfs.ReadCnt));
      m.put(Coname[Conidx_ant], String.valueOf(tfs.AntennaID));
      m.put(Coname[Conidx_pro], "");
      m.put(Coname[Conidx_rssi], String.valueOf(tfs.RSSI));
      m.put(Coname[Conidx_fre], String.valueOf(tfs.Frequency));

      m.put(Coname[Conidx_u8tid], tid);
      m.put(Coname[Conidx_u8bid], bid);

      m.put(Coname[Conidx_rfu], rfu);
      m.put(Coname[Conidx_tis], String.valueOf(tfs.TimeStamp));
      if (myapp.nxpu8 == 0)
        m.put(Coname[Conidx_data], emdstr);

      ListMs.add(m);

    } else {

      Reader.TAGINFO tf = TagsMap.get(key);

      for (int k = 0; k < ListMs.size(); k++) {
        @SuppressWarnings("unchecked")
        Map<String, String> m = (Map<String, String>) ListMs.get(k);

        boolean bl = false;
        if (myapp.isconngoods
          && myapp.listName.containsKey(epcstr.trim())) {
          if (m.get(Coname[Conidx_epcid]).equals(
            myapp.listName.get(epcstr.trim()))) {

            if (myapp.isUniByEmd) {
              if (myapp.nxpu8 == 0) {
                if (m.get(Coname[Conidx_data]) == null
                  || m.get(Coname[Conidx_data]).isEmpty()
                  || emdstr.equals(m.get(Coname[Conidx_data])))
                  bl = true;
              } else {
                if (m.get(Coname[Conidx_u8tid]) == null
                  || m.get(Coname[Conidx_u8tid]).isEmpty()
                  || tid.equals(m.get(Coname[Conidx_u8tid])))
                  bl = true;
              }
            } else
              bl = true;

          }
        } else {
          if (m.get(Coname[Conidx_epcid]).equals(epcstr)) {

            if (myapp.isUniByEmd) {
              if (myapp.nxpu8 == 0) {
                if (m.get(Coname[Conidx_data]) == null
                  || m.get(Coname[Conidx_data]).isEmpty()
                  || emdstr.equals(m.get(Coname[Conidx_data])))
                  bl = true;
              } else {
                if (m.get(Coname[Conidx_u8tid]) == null
                  || m.get(Coname[Conidx_u8tid]).isEmpty()
                  || tid.equals(m.get(Coname[Conidx_u8tid])))
                  bl = true;
              }

            }
            if (myapp.isUniByAnt) {
              if (String.valueOf(tfs.AntennaID).equals(
                m.get(Coname[3])))
                bl = true;
            } else
              bl = true;

          }
        }

        if (bl) {
          tf.ReadCnt += tfs.ReadCnt;
          tf.RSSI = tfs.RSSI;
          tf.Frequency = tfs.Frequency;

          m.put(Coname[Conidx_count], String.valueOf(tf.ReadCnt));
          m.put(Coname[Conidx_ant], String.valueOf(tfs.AntennaID));
          m.put(Coname[Conidx_rssi], String.valueOf(tf.RSSI));
          m.put(Coname[Conidx_fre], String.valueOf(tf.Frequency));
          m.put(Coname[Conidx_u8tid], tid);
          m.put(Coname[Conidx_rfu], rfu);
          m.put(Coname[Conidx_tis], String.valueOf(tfs.TimeStamp));
          if (myapp.nxpu8 == 0 && !emdstr.isEmpty())
            m.put(Coname[Conidx_data], emdstr);
          break;
        }
        // Fire callback on read button
      }
    }
  }

  private void customTagsBufferResh(PluginCall call, String EPC, Reader.TAGINFO tfs) {

    if (call == null) {
      return;
    }

    if (myapp.VisStatics)
      fcastatic(EPC);

    String key = EPC;
    String epcstr = EPC;
    String tid = "", bid = "";
    if (myapp.nxpu8 == 1) {
      key = key.substring(0, key.length() - 4);
      bid = epcstr.substring(epcstr.length() - 4, epcstr.length());
      epcstr = epcstr.substring(0, epcstr.length() - 4);
    } else if (myapp.nxpu8 == 2) {
      key = key.substring(0, key.length() - 24);
      tid = epcstr.substring(epcstr.length() - 24, epcstr.length());
      epcstr = epcstr.substring(0, epcstr.length() - 24);
    } else if (myapp.nxpu8 == 3) {
      key = key.substring(0, key.length() - 4);
      bid = epcstr.substring(epcstr.length() - 4, epcstr.length());
      epcstr = epcstr.substring(0, epcstr.length() - 4);
    }

    if (epcstr.length() < 24)
      epcstr = String.format("%-24s", epcstr);

    String emdstr = "";
    String rfu = "";

    if (tfs.EmbededDatalen > 0) {
      char[] out = new char[tfs.EmbededDatalen * 2];
      myapp.Mreader.Hex2Str(tfs.EmbededData, tfs.EmbededDatalen, out);
      emdstr = String.valueOf(out);
      if (myapp.nxpu8 == 1)
        tid = emdstr;
    }

    if (myapp.nxpu8 != 0) {
      char[] out2 = new char[4];
      myapp.Mreader.Hex2Str(tfs.Res, 2, out2);
      rfu = String.valueOf(out2);
    } else {
      //rfu=String.valueOf(((tfs.Res[0]<<8|tfs.Res[1])&0x3f)*180/64);
      rfu = String.valueOf(tfs.Res[1] & 0x3f);
      //rfu=String.valueOf((tfs.Res[1]&0x3f)*180/64);
    }

    if (myapp.isUniByEmd) {
      if (myapp.nxpu8 == 2) {
        key += tid;// nxpu8==2 epcid本身包含tid(nxpu8==2 epcid contain tid)
      } else {
        if (emdstr.isEmpty())
          return;

        if (myapp.nxpu8 != 2)
          key += emdstr;
      }

    }

    if (myapp.isUniByAnt)
      key += String.valueOf(tfs.AntennaID);

    if (!TagsMap.containsKey(key)) {

      if (locktagsm.tryLock()) {
        TagsMap.put(key, tfs);

        totalcount++;
        locktagsm.unlock();
      }


      // show
      Map<String, String> m = new HashMap<String, String>();
      m.put(Coname[Conidx_sort], String.valueOf(TagsMap.size()));

      if (myapp.isconngoods && myapp.listName.containsKey(epcstr.trim()))
        m.put(Coname[Conidx_epcid], myapp.listName.get(epcstr.trim()));
      else
        m.put(Coname[Conidx_epcid], epcstr);

      m.put(Coname[Conidx_count], String.valueOf(tfs.ReadCnt));
      m.put(Coname[Conidx_ant], String.valueOf(tfs.AntennaID));
      m.put(Coname[Conidx_pro], "");
      m.put(Coname[Conidx_rssi], String.valueOf(tfs.RSSI));
      m.put(Coname[Conidx_fre], String.valueOf(tfs.Frequency));

      m.put(Coname[Conidx_u8tid], tid);
      m.put(Coname[Conidx_u8bid], bid);

      m.put(Coname[Conidx_rfu], rfu);
      m.put(Coname[Conidx_tis], String.valueOf(tfs.TimeStamp));
      if (myapp.nxpu8 == 0)
        m.put(Coname[Conidx_data], emdstr);

      ListMs.add(m);

    } else {

      Reader.TAGINFO tf = TagsMap.get(key);

      for (int k = 0; k < ListMs.size(); k++) {
        @SuppressWarnings("unchecked")
        Map<String, String> m = (Map<String, String>) ListMs.get(k);

        boolean bl = false;
        if (myapp.isconngoods
          && myapp.listName.containsKey(epcstr.trim())) {
          if (m.get(Coname[Conidx_epcid]).equals(
            myapp.listName.get(epcstr.trim()))) {

            if (myapp.isUniByEmd) {
              if (myapp.nxpu8 == 0) {
                if (m.get(Coname[Conidx_data]) == null
                  || m.get(Coname[Conidx_data]).isEmpty()
                  || emdstr.equals(m.get(Coname[Conidx_data])))
                  bl = true;
              } else {
                if (m.get(Coname[Conidx_u8tid]) == null
                  || m.get(Coname[Conidx_u8tid]).isEmpty()
                  || tid.equals(m.get(Coname[Conidx_u8tid])))
                  bl = true;
              }
            } else
              bl = true;

          }
        } else {
          if (m.get(Coname[Conidx_epcid]).equals(epcstr)) {

            if (myapp.isUniByEmd) {
              if (myapp.nxpu8 == 0) {
                if (m.get(Coname[Conidx_data]) == null
                  || m.get(Coname[Conidx_data]).isEmpty()
                  || emdstr.equals(m.get(Coname[Conidx_data])))
                  bl = true;
              } else {
                if (m.get(Coname[Conidx_u8tid]) == null
                  || m.get(Coname[Conidx_u8tid]).isEmpty()
                  || tid.equals(m.get(Coname[Conidx_u8tid])))
                  bl = true;
              }

            }
            if (myapp.isUniByAnt) {
              if (String.valueOf(tfs.AntennaID).equals(
                m.get(Coname[3])))
                bl = true;
            } else
              bl = true;

          }
        }

        if (bl) {
          tf.ReadCnt += tfs.ReadCnt;
          tf.RSSI = tfs.RSSI;
          tf.Frequency = tfs.Frequency;

          m.put(Coname[Conidx_count], String.valueOf(tf.ReadCnt));
          m.put(Coname[Conidx_ant], String.valueOf(tfs.AntennaID));
          m.put(Coname[Conidx_rssi], String.valueOf(tf.RSSI));
          m.put(Coname[Conidx_fre], String.valueOf(tf.Frequency));
          m.put(Coname[Conidx_u8tid], tid);
          m.put(Coname[Conidx_rfu], rfu);
          m.put(Coname[Conidx_tis], String.valueOf(tfs.TimeStamp));
          if (myapp.nxpu8 == 0 && !emdstr.isEmpty())
            m.put(Coname[Conidx_data], emdstr);
          break;
        }
        // Fire callback on read button
        Gson gson = new Gson();
        String json = gson.toJson(ListMs);
        JSObject object = new JSObject();
        object.put("data", json);
//        Log.e("data", String.valueOf(object));
        call.resolve(object);
      }
    }
  }

  @Override
  public void handleOnDestroy() {
    Awl.ReleaseWakeLock();
    getActivity().unregisterReceiver(mBroadcastReceiver);

    if (myapp.isReport_rec) {
      try {
        if (myapp.fs != null)
          myapp.fs.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    System.exit(0);
    super.handleOnDestroy();
  }

  private void ConnectHandleUI() {
    try {
      Reader.READER_ERR er;
      myapp.Rparams = myapp.spf.ReadReaderParams();
      if (isdefaultmaxpow_V) {
        int[] mp = new int[1];
        er = myapp.Mreader.ParamGet(
          Reader.Mtr_Param.MTR_PARAM_RF_MAXPOWER, mp);
        if (er == Reader.READER_ERR.MT_OK_ERR) {
          myapp.Rparams.setdefaulpwval((short) mp[0]);
        }
      }

      if (myapp.Rparams.invpro.size() < 1)
        myapp.Rparams.invpro.add("GEN2");

      List<Reader.SL_TagProtocol> ltp = new ArrayList<Reader.SL_TagProtocol>();
      for (int i = 0; i < myapp.Rparams.invpro.size(); i++) {
        if (myapp.Rparams.invpro.get(i).equals("GEN2")) {
          ltp.add(Reader.SL_TagProtocol.SL_TAG_PROTOCOL_GEN2);

        } else if (myapp.Rparams.invpro.get(i).equals("6B")) {
          ltp.add(Reader.SL_TagProtocol.SL_TAG_PROTOCOL_ISO180006B);

        } else if (myapp.Rparams.invpro.get(i).equals("IPX64")) {
          ltp.add(Reader.SL_TagProtocol.SL_TAG_PROTOCOL_IPX64);

        } else if (myapp.Rparams.invpro.get(i).equals("IPX256")) {
          ltp.add(Reader.SL_TagProtocol.SL_TAG_PROTOCOL_IPX256);
        }
      }

      Reader.Inv_Potls_ST ipst = myapp.Mreader.new Inv_Potls_ST();
      ipst.potlcnt = ltp.size();
      ipst.potls = new Reader.Inv_Potl[ipst.potlcnt];
      Reader.SL_TagProtocol[] stp = ltp
        .toArray(new Reader.SL_TagProtocol[ipst.potlcnt]);
      for (int i = 0; i < ipst.potlcnt; i++) {
        Reader.Inv_Potl ipl = myapp.Mreader.new Inv_Potl();
        ipl.weight = 30;
        ipl.potl = stp[i];
        ipst.potls[i] = ipl;
      }

      er = myapp.Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_INVPOTL, ipst);
      Log.d("MYINFO", "Connected set pro:" + er.toString());

      er = myapp.Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_READER_IS_CHK_ANT,
        new int[]{myapp.Rparams.checkant});
      Log.d("MYINFO", "Connected set checkant:" + er.toString());

      Reader.AntPowerConf apcf = myapp.Mreader.new AntPowerConf();
      apcf.antcnt = myapp.antportc;
      for (int i = 0; i < apcf.antcnt; i++) {
        Reader.AntPower jaap = myapp.Mreader.new AntPower();
        jaap.antid = i + 1;
        jaap.readPower = (short) myapp.Rparams.rpow[i];
        jaap.writePower = (short) myapp.Rparams.wpow[i];
        apcf.Powers[i] = jaap;
      }
      nowpower = myapp.Rparams.rpow[0];
      myapp.Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_RF_ANTPOWER, apcf);

      Reader.Region_Conf rre;
      switch (myapp.Rparams.region) {
        case 0:
          rre = Reader.Region_Conf.RG_PRC;
          break;
        case 1:
          rre = Reader.Region_Conf.RG_NA;
          break;
        case 2:
          rre = Reader.Region_Conf.RG_NONE;
          break;
        case 3:
          rre = Reader.Region_Conf.RG_KR;
          break;
        case 4:
          rre = Reader.Region_Conf.RG_EU;
          break;
        case 5:
          rre = Reader.Region_Conf.RG_EU2;
          break;
        case 6:
          rre = Reader.Region_Conf.RG_EU3;
          break;
        case 9:
          rre = Reader.Region_Conf.RG_OPEN;
          break;
        case 10:
          rre = Reader.Region_Conf.RG_PRC2;
          break;
        case 7:
        case 8:
        default:
          rre = Reader.Region_Conf.RG_NONE;
          break;
      }
      if (rre != Reader.Region_Conf.RG_NONE) {
        er = myapp.Mreader.ParamSet(
          Reader.Mtr_Param.MTR_PARAM_FREQUENCY_REGION, rre);
      }

      if (myapp.Rparams.frelen > 0) {

        Reader.HoptableData_ST hdst = myapp.Mreader.new HoptableData_ST();
        hdst.lenhtb = myapp.Rparams.frelen;
        hdst.htb = myapp.Rparams.frecys;
        er = myapp.Mreader.ParamSet(
          Reader.Mtr_Param.MTR_PARAM_FREQUENCY_HOPTABLE, hdst);
      }

      er = myapp.Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_SESSION,
        new int[]{myapp.Rparams.session});
      er = myapp.Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_Q,
        new int[]{myapp.Rparams.qv});
      er = myapp.Mreader.ParamSet(
        Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_WRITEMODE,
        new int[]{myapp.Rparams.wmode});
      er = myapp.Mreader.ParamSet(
        Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_MAXEPCLEN,
        new int[]{myapp.Rparams.maxlen});
      er = myapp.Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_TARGET,
        new int[]{myapp.Rparams.target});

      if (myapp.Rparams.filenable == 1) {
        Reader.TagFilter_ST tfst = myapp.Mreader.new TagFilter_ST();
        tfst.bank = myapp.Rparams.filbank;
        int len = myapp.Rparams.fildata.length();
        len = len % 2 == 0 ? len : len + 1;
        tfst.fdata = new byte[len / 2];
        myapp.Mreader.Str2Hex(myapp.Rparams.fildata,
          myapp.Rparams.fildata.length(), tfst.fdata);
        tfst.flen = myapp.Rparams.fildata.length() * 4;
        tfst.startaddr = myapp.Rparams.filadr;
        tfst.isInvert = myapp.Rparams.filisinver;

        myapp.Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_FILTER, tfst);
      }

      if (myapp.Rparams.emdenable == 1) {
        Reader.EmbededData_ST edst = myapp.Mreader.new EmbededData_ST();

        edst.accesspwd = null;
        edst.bank = myapp.Rparams.emdbank;
        edst.startaddr = myapp.Rparams.emdadr;
        edst.bytecnt = myapp.Rparams.emdbytec;

        er = myapp.Mreader.ParamSet(
          Reader.Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, edst);
      }

      er = myapp.Mreader.ParamSet(
        Reader.Mtr_Param.MTR_PARAM_TAGDATA_UNIQUEBYEMDDATA,
        new int[]{myapp.Rparams.adataq});
      er = myapp.Mreader.ParamSet(
        Reader.Mtr_Param.MTR_PARAM_TAGDATA_RECORDHIGHESTRSSI,
        new int[]{myapp.Rparams.rhssi});
      er = myapp.Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_SEARCH_MODE,
        new int[]{myapp.Rparams.invw});

      Reader.AntPortsVSWR apvr = myapp.Mreader.new AntPortsVSWR();
      apvr.andid = 1;
      apvr.power = (short) myapp.Rparams.rpow[0];
      apvr.region = Reader.Region_Conf.RG_NA;
      er = myapp.Mreader.ParamGet(Reader.Mtr_Param.MTR_PARAM_RF_ANTPORTS_VSWR, apvr);

      Reader.HardwareDetails val = myapp.Mreader.new HardwareDetails();
      er = myapp.Mreader.GetHardwareDetails(val);

      if (er == Reader.READER_ERR.MT_OK_ERR) {
        myapp.myhd = val;
      }
    } catch (Exception ex) {
      Log.d("MYINFO",
        ex.getMessage() + ex.toString() + ex.getStackTrace());
    }
  }

  String[] Coname;

  private void SortbyEpc() {
    List<Map<String, ?>> ListMsnohead = new ArrayList<Map<String, ?>>();

    for (int i = 1; i < ListMs.size(); i++)
      ListMsnohead.add(ListMs.get(i));

    Collections.sort(ListMsnohead, new Comparator<Map<String, ?>>() {

      @Override
      public int compare(Map<String, ?> arg0, Map<String, ?> arg1) {
        // TODO Auto-generated method stub
        return ((String) arg0.get(Coname[Conidx_epcid])).compareTo((String) arg1
          .get(Coname[Conidx_epcid]));

      }
    });
    ListMs.clear();
    ListMs.add(h);
    ListMs.addAll(ListMsnohead);
  }

  public void StopFunc(boolean isfinal) {
    if (myapp.ThreadMODE == 0) {

      if (myapp.issmartmode) {
        Reader.READER_ERR er = myapp.Mreader.AsyncStopReading_IT_CT();
        if (er != Reader.READER_ERR.MT_OK_ERR) {
          myapp.Mreader.GetLastDetailError(myapp.ei);
          Log.e("error", "Failed to stop counting without pausing");
          return;
        }
      } else {
        if (myapp.isquicklymode) {
          Reader.READER_ERR er = myapp.Mreader.AsyncStopReading();
          if (er != Reader.READER_ERR.MT_OK_ERR) {
            myapp.Mreader.GetLastDetailError(myapp.ei);
            Log.e("error", "Failed to stop counting without pausing");
            return;
          }
        }
        handler.removeCallbacks(runnable_MainActivity);
      }
    } else if (myapp.ThreadMODE == 1) {
      if (myapp.Mreader.StopReading() != Reader.READER_ERR.MT_OK_ERR) {
        myapp.Mreader.GetLastDetailError(myapp.ei);
        Log.e("error", "Failed to stop counting without pausing");
        return;
      }
    }

    if (myapp.VisTestFor) {
      if (Testcount >= myapp.Vtestforcount) {
//        dlog.toDlog("test count reach");
        isrun = false;
      }
    } else {  //dlog.toDlog("test count 1 reach");
      isrun = false;
    }

    if (isfinal)
      isrun = false;

    Awl.ReleaseWakeLock();

    myapp.TagsMap.putAll(TagsMap);

    if (myapp.VisStatics && isrun == false) { // vstatichandler.removeCallbacks(runnable_statics);
      try {
        staticthread.join();
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    if (myapp.isEpcup && isrun == false) {
      SortbyEpc();
    }


    // 时间数量报表
    if (myapp.isReport_rec) {
      Vallcount += totalcount;
      Valltime += runtime;

      // 导出xls文件
      {
        if (Scount != 0 || VstaticTags.size() != 0) {
          try {
            dexel.WriteExcelfile(new Object[]{
              (Integer) (Testcount),
              (Integer) Scount,
              (Integer) VstaticTags.size(),
              (Integer) totalcount - totalcountlast,
              (Integer) totalcount,
              (Long) runtime});

          } catch (WriteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      }
      dexel.Addline(2);

      VstaticTags.clear();
      Scount = 0;
      totalcountlast = 0;
      totalcount = 0;

      if (!myapp.VisTestFor || (myapp.VisTestFor && Testcount >= myapp.Vtestforcount)) {
        String linestr = myapp.myhd.module.toString() + "   ";
        int cnp = nowpower;
        linestr += String.valueOf(cnp);
        int divd = myapp.Vtestforcount == 0 ? 1 : myapp.Vtestforcount;
        linestr += "  平均标签数量："
          + String.valueOf((int) Vallcount / divd)
          + "\r\n平均耗时："
          + String.valueOf((int) Valltime / divd)
          + "  \r\n日期：" + getcurDate()
          + "\r\n\r\n\r\n";

        dexel.mergeandtext(0, dexel.GetY(), 5,
          dexel.GetY(), linestr);

        Vallcount = 0;
        Valltime = 0;
      }
    }

    if (myapp.isReport_tep) {
      int[] val = new int[1];
      val[0] = 0;

      Object[] objs = new Object[3];
      objs[0] = getcurDate();
      Reader.READER_ERR ert = myapp.Mreader.ParamGet(
        Reader.Mtr_Param.MTR_PARAM_RF_TEMPERATURE, val);
      objs[1] = val[0];
      objs[2] = batt_level;

      try {
        dexel.WriteExcelfile(objs);
      } catch (WriteException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    if (isrun == false) {
      if (myapp.isReport_rec || myapp.isReport_tep) {
        if (myapp.isReport_tep) {
          mBroadcastReceiver = new MyBroadcastReceiver();
          IntentFilter intentFilter = new IntentFilter();
          intentFilter.addAction(BROADCAST_ACTION1);
          intentFilter.addAction(BROADCAST_ACTION2);
          intentFilter.addAction(BROADCAST_ACTION3);
          getActivity().registerReceiver(mBroadcastReceiver, intentFilter);

          int[] val = new int[1];
          val[0] = 0;

          Object[] objs = new Object[3];
          objs[0] = getcurDate();
          Reader.READER_ERR ert = myapp.Mreader.ParamGet(
            Reader.Mtr_Param.MTR_PARAM_RF_TEMPERATURE, val);
          objs[1] = val[0];
          objs[2] = batt_level;
        }

        try {
          dexel.SaveandCloseExcelfile();
        } catch (WriteException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

      }
    }

    if (myapp.isReport_pos) {
      DjxlExcel dexel = new DjxlExcel("标签识读报表");
      try {
        Iterator<Map.Entry<String, Reader.TAGINFO>> iesb;
        iesb = myapp.TagsMap.entrySet().iterator();
        List<String> lstr = new ArrayList<String>();
        while (iesb.hasNext()) {
          Reader.TAGINFO bd = iesb.next().getValue();
          String linestr = Reader.bytes_Hexstr(bd.EpcId);
          lstr.add(linestr);
        }

        dexel.CreateTestBoxExcelfile_v2(lstr);
      } catch (IOException e) {
        // TODO Auto-generated catch block
//        constPluginCall.reject(e.getMessage());
      } catch (WriteException e) {
        // TODO Auto-generated catch block
//        constPluginCall.reject(e.getMessage());
      }
    }
  }

  private void fcastatic(String epcid) {

    if (lockobj.tryLock()) {
      try {
        // 处理任务
        Scount++;
        if (!VstaticTags.contains(epcid)) {
          VstaticTags.add(epcid);
        }

      } catch (Exception ex) {

      } finally {
        lockobj.unlock(); // 释放锁
      }
    } else {
      // 如果不能获取锁，则直接做其他事情
    }
  }

  public interface ScreenStateListener {// 返回给调用者屏幕状态信息

    public void onScreenOn();

    public void onScreenOff();

    // public void onUserPresent();
  }

  public static boolean runRootCommand(String command) {
    Process process = null;
    DataOutputStream os = null;

    try {
      process = Runtime.getRuntime().exec("sh");// Runtime.getRuntime().exec("su");
      os = new DataOutputStream(process.getOutputStream());
      os.writeBytes(command + "\n");
      os.writeBytes("exit\n");
      os.flush();
      process.waitFor();
    } catch (Exception e) {
      Log.d("Phone Link",
        "su root - the device is not rooted,  error message： "
          + e.getMessage());
      return false;
    } finally {
      try {
        if (null != os) {
          os.close();
        }
        if (null != process) {
          process.destroy();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return true;
  }

  private boolean reconnect() {
    boolean blen = myapp.Rpower.PowerUp();
    if (!blen)
      return false;
    System.out.println(MyApplication.Constr_mainpu);

    Reader.READER_ERR er = myapp.Mreader.InitReader_Notype(myapp.Address,
      myapp.antportc);
    if (er == Reader.READER_ERR.MT_OK_ERR) {
      myapp.needreconnect = false;
      try {
        // myapp.Rparams = myapp.spf.ReadReaderParams();

        if (myapp.Rparams.invpro.size() < 1)
          myapp.Rparams.invpro.add("GEN2");

        List<Reader.SL_TagProtocol> ltp = new ArrayList<Reader.SL_TagProtocol>();
        for (int i = 0; i < myapp.Rparams.invpro.size(); i++) {
          if (myapp.Rparams.invpro.get(i).equals("GEN2")) {
            ltp.add(Reader.SL_TagProtocol.SL_TAG_PROTOCOL_GEN2);

          } else if (myapp.Rparams.invpro.get(i).equals("6B")) {
            ltp.add(Reader.SL_TagProtocol.SL_TAG_PROTOCOL_ISO180006B);

          } else if (myapp.Rparams.invpro.get(i).equals("IPX64")) {
            ltp.add(Reader.SL_TagProtocol.SL_TAG_PROTOCOL_IPX64);

          } else if (myapp.Rparams.invpro.get(i).equals("IPX256")) {
            ltp.add(Reader.SL_TagProtocol.SL_TAG_PROTOCOL_IPX256);

          }
        }

        Reader.Inv_Potls_ST ipst = myapp.Mreader.new Inv_Potls_ST();
        ipst.potlcnt = ltp.size();
        ipst.potls = new Reader.Inv_Potl[ipst.potlcnt];
        Reader.SL_TagProtocol[] stp = ltp
          .toArray(new Reader.SL_TagProtocol[ipst.potlcnt]);
        for (int i = 0; i < ipst.potlcnt; i++) {
          Reader.Inv_Potl ipl = myapp.Mreader.new Inv_Potl();
          ipl.weight = 30;
          ipl.potl = stp[i];
          ipst.potls[0] = ipl;
        }

        er = myapp.Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_INVPOTL,
          ipst);
        Log.d("MYINFO", "Connected set pro:" + er.toString());

        er = myapp.Mreader.ParamSet(
          Reader.Mtr_Param.MTR_PARAM_READER_IS_CHK_ANT,
          new int[]{myapp.Rparams.checkant});
        Log.d("MYINFO", "Connected set checkant:" + er.toString());

        Reader.AntPowerConf apcf = myapp.Mreader.new AntPowerConf();
        apcf.antcnt = myapp.antportc;
        for (int i = 0; i < apcf.antcnt; i++) {
          Reader.AntPower jaap = myapp.Mreader.new AntPower();
          jaap.antid = i + 1;
          jaap.readPower = (short) myapp.Rparams.rpow[i];
          jaap.writePower = (short) myapp.Rparams.wpow[i];
          apcf.Powers[i] = jaap;
        }

        myapp.Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_RF_ANTPOWER, apcf);

        Reader.Region_Conf rre;
        switch (myapp.Rparams.region) {
          case 0:
            rre = Reader.Region_Conf.RG_PRC;
            break;
          case 1:
            rre = Reader.Region_Conf.RG_NA;
            break;
          case 2:
            rre = Reader.Region_Conf.RG_NONE;
            break;
          case 3:
            rre = Reader.Region_Conf.RG_KR;
            break;
          case 4:
            rre = Reader.Region_Conf.RG_EU;
            break;
          case 5:
          case 6:
          case 7:
          case 8:
          default:
            rre = Reader.Region_Conf.RG_NONE;
            break;
        }
        if (rre != Reader.Region_Conf.RG_NONE) {
          er = myapp.Mreader.ParamSet(
            Reader.Mtr_Param.MTR_PARAM_FREQUENCY_REGION, rre);
        }

        if (myapp.Rparams.frelen > 0) {

          Reader.HoptableData_ST hdst = myapp.Mreader.new HoptableData_ST();
          hdst.lenhtb = myapp.Rparams.frelen;
          hdst.htb = myapp.Rparams.frecys;
          er = myapp.Mreader.ParamSet(
            Reader.Mtr_Param.MTR_PARAM_FREQUENCY_HOPTABLE, hdst);
        }

        er = myapp.Mreader.ParamSet(
          Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_SESSION,
          new int[]{myapp.Rparams.session});
        er = myapp.Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_Q,
          new int[]{myapp.Rparams.qv});
        er = myapp.Mreader.ParamSet(
          Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_WRITEMODE,
          new int[]{myapp.Rparams.wmode});
        er = myapp.Mreader.ParamSet(
          Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_MAXEPCLEN,
          new int[]{myapp.Rparams.maxlen});
        er = myapp.Mreader.ParamSet(
          Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_TARGET,
          new int[]{myapp.Rparams.target});

        er = myapp.Mreader.ParamSet(
          Reader.Mtr_Param.MTR_PARAM_TAGDATA_UNIQUEBYEMDDATA,
          new int[]{myapp.Rparams.adataq});
        er = myapp.Mreader.ParamSet(
          Reader.Mtr_Param.MTR_PARAM_TAGDATA_RECORDHIGHESTRSSI,
          new int[]{myapp.Rparams.rhssi});
        er = myapp.Mreader.ParamSet(
          Reader.Mtr_Param.MTR_PARAM_TAG_SEARCH_MODE,
          new int[]{myapp.Rparams.invw});

        Reader.NXP_U8_InventoryModePara u8para = myapp.Mreader.new NXP_U8_InventoryModePara();

        u8para.Mode[0] = 0;

        if (myapp.nxpu8 == 0) {
          if (myapp.Rparams.filenable == 1) {
            Reader.TagFilter_ST tfst = myapp.Mreader.new TagFilter_ST();
            tfst.bank = myapp.Rparams.filbank;
            int len = myapp.Rparams.fildata.length();
            len = len % 2 == 0 ? len : len + 1;
            tfst.fdata = new byte[len / 2];
            myapp.Mreader.Str2Hex(myapp.Rparams.fildata,
              myapp.Rparams.fildata.length(), tfst.fdata);
            tfst.flen = myapp.Rparams.fildata.length() * 4;
            tfst.startaddr = myapp.Rparams.filadr;
            tfst.isInvert = myapp.Rparams.filisinver;

            myapp.Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_FILTER,
              tfst);
          }

          if (myapp.Rparams.emdenable == 1) {
            Reader.EmbededData_ST edst = myapp.Mreader.new EmbededData_ST();

            edst.accesspwd = null;
            edst.bank = myapp.Rparams.emdbank;
            edst.startaddr = myapp.Rparams.emdadr;
            edst.bytecnt = myapp.Rparams.emdbytec;

            er = myapp.Mreader.ParamSet(
              Reader.Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, edst);
          }
        } else if (myapp.nxpu8 == 1) {
          Reader.TagFilter_ST tfst = myapp.Mreader.new TagFilter_ST();

          tfst.fdata = new byte[1];
          tfst.fdata[0] = (byte) 0x80;
          tfst.bank = 1;
          tfst.flen = 1;
          tfst.startaddr = 0x204;
          tfst.isInvert = 0;
          er = myapp.Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_FILTER,
            tfst);

          Reader.EmbededData_ST edst = myapp.Mreader.new EmbededData_ST();
          edst.accesspwd = null;

          edst.bank = 2;
          edst.startaddr = 0;
          edst.bytecnt = 12;

          er = myapp.Mreader.ParamSet(
            Reader.Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, edst);
          if (er == Reader.READER_ERR.MT_OK_ERR) {
          }

          u8para.Mode[0] = 1;
          myapp.nxpu8 = 1;
        } else if (myapp.nxpu8 == 2) {
          Reader.TagFilter_ST tfst = myapp.Mreader.new TagFilter_ST();

          tfst.fdata = new byte[1];
          tfst.fdata[0] = (byte) 0x80;
          tfst.bank = 1;
          tfst.flen = 1;
          tfst.startaddr = 0x203;
          tfst.isInvert = 0;
          er = myapp.Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_FILTER,
            tfst);

          u8para.Mode[0] = 1;
          myapp.nxpu8 = 2;
        } else if (myapp.nxpu8 == 3) {
          Reader.TagFilter_ST tfst = myapp.Mreader.new TagFilter_ST();

          tfst.fdata = new byte[1];
          tfst.fdata[0] = (byte) 0x80;
          tfst.bank = 1;
          tfst.flen = 1;
          tfst.startaddr = 0x204;
          tfst.isInvert = 0;
          er = myapp.Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_FILTER,
            tfst);
          u8para.Mode[0] = 1;
          myapp.nxpu8 = 3;
        }
        myapp.Mreader.CustomCmd(0, Reader.CustomCmdType.NXP_U8_InventoryMode,
          u8para, null);

      } catch (Exception ex) {
        Log.d("MYINFO",
          ex.getMessage() + ex.toString() + ex.getStackTrace());
      }
    } else
      return false;

    return true;
  }

  public static String getcurDate() {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日   HH:mm:ss");
    Date curDate = new Date(System.currentTimeMillis());
    return formatter.format(curDate);
  }
}
