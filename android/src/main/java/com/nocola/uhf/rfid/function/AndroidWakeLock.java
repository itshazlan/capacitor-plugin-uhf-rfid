package com.nocola.uhf.rfid.function;

import android.os.PowerManager;

public class AndroidWakeLock {
  PowerManager.WakeLock wakelock;
  PowerManager pmr;

  public AndroidWakeLock(PowerManager pm) {

    // PowerManager pm = (PowerManager)
    // getSystemService(Context.POWER_SERVICE);
    pmr = pm;
  }

  public void WakeLock() {
    if (wakelock == null) {
      wakelock = pmr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this
        .getClass().getCanonicalName());
    }
    wakelock.acquire();
  }

  public void ReleaseWakeLock() {
    if (wakelock != null && wakelock.isHeld()) {
      wakelock.release();
      wakelock = null;
    }
  }
}
