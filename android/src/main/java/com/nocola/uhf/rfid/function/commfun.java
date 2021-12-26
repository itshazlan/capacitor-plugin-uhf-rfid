package com.nocola.uhf.rfid.function;

import android.view.View;
import android.widget.RadioGroup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class commfun {
    public static int SortGroup(RadioGroup rg) {
        int check1 = rg.getCheckedRadioButtonId();
        if (check1 != -1) {
            for (int i = 0; i < rg.getChildCount(); i++) {
                View vi = rg.getChildAt(i);
                int vv = vi.getId();
                if (check1 == vv) {
                    return i;
                }
            }

            return -1;
        } else
            return check1;
    }

    public static int[] Sort(int[] array, int len) {
        int tmpIntValue = 0;
        for (int xIndex = 0; xIndex < len; xIndex++) {
            for (int yIndex = 0; yIndex < len; yIndex++) {
                if (array[xIndex] < array[yIndex]) {
                    tmpIntValue = (Integer) array[xIndex];
                    array[xIndex] = array[yIndex];
                    array[yIndex] = tmpIntValue;
                }
            }
        }
        int[] reary = new int[len];
        System.arraycopy(array, 0, reary, 0, len);
        return reary;
    }

    public static int[] CollectionTointArray(
            @SuppressWarnings("rawtypes") List list) {
        @SuppressWarnings("rawtypes")
        Iterator itor = list.iterator();
        int[] backdata = new int[list.size()];
        int i = 0;
        while (itor.hasNext()) {
            backdata[i++] = (int) (Integer) itor.next();
        }
        return backdata;
    }

    public static String getcurDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日   HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());
        return formatter.format(curDate);
    }
}
