package com.nocola.uhf.rfid.function;

import android.content.Context;
import android.content.SharedPreferences;

import com.nocola.uhf.rfid.MyApplication;
import com.nocola.uhf.rfid.MyApplication.ReaderParams;
import com.nocola.uhf.rfid.RFIDPlugin;

public class SPconfig {
    SharedPreferences sp;

    public SPconfig(RFIDPlugin cth) {
        Context ctx = cth.getContext();
        sp = ctx.getSharedPreferences("SP",
                android.content.Context.MODE_PRIVATE);
    }

    public String GetString(String key) {
        return sp.getString(key, "");
    }

    public boolean SaveString(String key, String val) {
        try {
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(key, val);
            editor.commit();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public void SaveReaderParams(MyApplication.ReaderParams RP) {
        SaveString("OPANT", String.valueOf(RP.opant));
        if (RP.invpro != null) {
            String[] pros = RP.invpro.toArray(new String[RP.invpro.size()]);
            String strval = "";
            for (int i = 0; i < pros.length; i++) {
                strval += pros[i] + ",";
            }
            if (!strval.equals("")) {
                strval = strval.substring(0, strval.length() - 1);
                SaveString("INVPRO", strval);
            }
        }
        if (RP.opro != null && !RP.opro.equals(""))
            SaveString("OPRO", RP.opro);

        if (RP.uants != null && RP.uants.length > 0) {
            String strval = "";
            for (int i = 0; i < RP.uants.length; i++) {
                strval += RP.uants[i] + ",";
            }
            if (!strval.equals("")) {
                strval = strval.substring(0, strval.length() - 1);
                SaveString("UANTS", strval);
            }
        }
        SaveString("READTIME", String.valueOf(RP.readtime));
        SaveString("SLEEP", String.valueOf(RP.sleep));
        SaveString("CHECKANT", String.valueOf(RP.checkant));
        if (RP.rpow != null && RP.rpow.length > 0) {
            String strval = "";
            for (int i = 0; i < RP.rpow.length; i++) {
                strval += RP.rpow[i] + ",";
            }
            if (!strval.equals("")) {
                strval = strval.substring(0, strval.length() - 1);
                SaveString("RPOW", strval);
            }
        }
        if (RP.wpow != null && RP.wpow.length > 0) {
            String strval = "";
            for (int i = 0; i < RP.wpow.length; i++) {
                strval += RP.wpow[i] + ",";
            }
            if (!strval.equals("")) {
                strval = strval.substring(0, strval.length() - 1);
                SaveString("WPOW", strval);
            }
        }

        SaveString("REGION", String.valueOf(RP.region));
        SaveString("FRELEN", String.valueOf(RP.frelen));
        if (RP.frelen > 0) {
            if (RP.frecys != null && RP.frecys.length > 0) {
                String strval = "";
                for (int i = 0; i < RP.frecys.length; i++) {
                    strval += RP.frecys[i] + ",";
                }
                if (!strval.equals("")) {
                    strval = strval.substring(0, strval.length() - 1);
                    SaveString("FRECYS", strval);
                }
            }
        }

        SaveString("SESSION", String.valueOf(RP.session));
        SaveString("QV", String.valueOf(RP.qv));
        SaveString("WMODE", String.valueOf(RP.wmode));
        SaveString("BLF", String.valueOf(RP.blf));
        SaveString("MAXLEN", String.valueOf(RP.maxlen));
        SaveString("TARGET", String.valueOf(RP.target));
        SaveString("GEN2CODE", String.valueOf(RP.gen2code));
        SaveString("GEN2TARI", String.valueOf(RP.gen2tari));

        if (RP.fildata != null && !RP.fildata.equals(""))
            SaveString("FILDATA", RP.fildata);

        SaveString("FILADR", String.valueOf(RP.filadr));
        SaveString("FILBANK", String.valueOf(RP.filbank));
        SaveString("FILISINVER", String.valueOf(RP.filisinver));
        SaveString("FILENABLE", String.valueOf(RP.filenable));

        SaveString("EMDADR", String.valueOf(RP.emdadr));
        SaveString("EMDBYTEC", String.valueOf(RP.emdbytec));
        SaveString("EMDBANK", String.valueOf(RP.emdbank));
        SaveString("EMDENABLE", String.valueOf(RP.emdenable));

        SaveString("ADATAQ", String.valueOf(RP.adataq));
        SaveString("RHSSI", String.valueOf(RP.rhssi));
        SaveString("INVW", String.valueOf(RP.invw));
        SaveString("ISO6BDEEP", String.valueOf(RP.iso6bdeep));
        SaveString("ISO6BDEL", String.valueOf(RP.iso6bdel));
        SaveString("ISO6BBLF", String.valueOf(RP.iso6bblf));

    }

    public MyApplication.ReaderParams ReadReaderParams() {
        ReaderParams RP = new MyApplication().new ReaderParams();
        if (GetString("OPANT") != null && !GetString("OPANT").equals(""))
            RP.opant = Integer.valueOf(GetString("OPANT"));

        if (GetString("INVPRO") != null && !GetString("INVPRO").equals("")) {
            RP.invpro.clear();
            String[] vals = GetString("INVPRO").split(",");

            for (int i = 0; i < vals.length; i++) {
                RP.invpro.add(vals[i]);
            }

        }
        if (GetString("OPRO") != null && !GetString("OPRO").equals(""))
            RP.opro = GetString("OPRO");

        if (GetString("UANTS") != null && !GetString("UANTS").equals("")) {
            String[] vals = GetString("UANTS").split(",");
            RP.uants = new int[vals.length];
            for (int i = 0; i < vals.length; i++) {
                RP.uants[i] = Integer.valueOf(vals[i]);
            }
        }
        if (GetString("READTIME") != null && !GetString("READTIME").equals(""))
            RP.readtime = Integer.valueOf(GetString("READTIME"));
        if (GetString("SLEEP") != null && !GetString("SLEEP").equals(""))
            RP.sleep = Integer.valueOf(GetString("SLEEP"));
        if (GetString("CHECKANT") != null && !GetString("CHECKANT").equals(""))
            RP.checkant = Integer.valueOf(GetString("CHECKANT"));

        if (GetString("RPOW") != null && !GetString("RPOW").equals("")) {
            String[] vals = GetString("RPOW").split(",");
            RP.rpow = new int[vals.length];
            for (int i = 0; i < vals.length; i++) {
                RP.rpow[i] = Integer.valueOf(vals[i]);
            }
        }
        if (GetString("WPOW") != null && !GetString("WPOW").equals("")) {
            String[] vals = GetString("WPOW").split(",");
            RP.wpow = new int[vals.length];
            for (int i = 0; i < vals.length; i++) {
                RP.wpow[i] = Integer.valueOf(vals[i]);
            }
        }
        if (GetString("REGION") != null && !GetString("REGION").equals(""))
            RP.region = Integer.valueOf(GetString("REGION"));
        else
            RP.region = 1;
        if (GetString("FRELEN") != null && !GetString("FRELEN").equals(""))
            RP.frelen = Integer.valueOf(GetString("FRELEN"));

        if (RP.frelen > 0) {
            if (GetString("FRECYS") != null && !GetString("FRECYS").equals("")) {
                String[] vals = GetString("FRECYS").split(",");
                RP.frecys = new int[RP.frelen];
                for (int i = 0; i < vals.length; i++) {
                    RP.frecys[i] = Integer.valueOf(vals[i]);
                }
            }
        }
        if (GetString("SESSION") != null && !GetString("SESSION").equals(""))
            RP.session = Integer.valueOf(GetString("SESSION"));
        if (GetString("QV") != null && !GetString("QV").equals(""))
            RP.qv = Integer.valueOf(GetString("QV"));
        if (GetString("WMODE") != null && !GetString("WMODE").equals(""))
            RP.wmode = Integer.valueOf(GetString("WMODE"));
        if (GetString("BLF") != null && !GetString("BLF").equals(""))
            RP.blf = Integer.valueOf(GetString("BLF"));
        if (GetString("MAXLEN") != null && !GetString("MAXLEN").equals(""))
            RP.maxlen = Integer.valueOf(GetString("MAXLEN"));
        if (GetString("TARGET") != null && !GetString("TARGET").equals(""))
            RP.target = Integer.valueOf(GetString("TARGET"));
        if (GetString("GEN2CODE") != null && !GetString("GEN2CODE").equals(""))
            RP.gen2code = Integer.valueOf(GetString("GEN2CODE"));
        if (GetString("GEN2TARI") != null && !GetString("GEN2TARI").equals(""))
            RP.gen2tari = Integer.valueOf(GetString("GEN2TARI"));

        if (GetString("FILDATA") != null && !GetString("FILDATA").equals(""))
            RP.fildata = GetString("FILDATA");
        if (GetString("FILADR") != null && !GetString("FILADR").equals(""))
            RP.filadr = Integer.valueOf(GetString("FILADR"));
        if (GetString("FILBANK") != null && !GetString("FILBANK").equals(""))
            RP.filbank = Integer.valueOf(GetString("FILBANK"));
        if (GetString("FILISINVER") != null
                && !GetString("FILISINVER").equals(""))
            RP.filisinver = Integer.valueOf(GetString("FILISINVER"));
        if (GetString("FILENABLE") != null
                && !GetString("FILENABLE").equals(""))
            RP.filenable = Integer.valueOf(GetString("FILENABLE"));

        if (GetString("FILENABLE") != null
                && !GetString("FILENABLE").equals(""))
            RP.emdadr = Integer.valueOf(GetString("EMDADR"));
        if (GetString("FILENABLE") != null
                && !GetString("FILENABLE").equals(""))
            RP.emdbytec = Integer.valueOf(GetString("EMDBYTEC"));
        if (GetString("FILENABLE") != null
                && !GetString("FILENABLE").equals(""))
            RP.emdbank = Integer.valueOf(GetString("EMDBANK"));
        if (GetString("FILENABLE") != null
                && !GetString("FILENABLE").equals(""))
            RP.emdenable = Integer.valueOf(GetString("EMDENABLE"));

        if (GetString("ADATAQ") != null && !GetString("ADATAQ").equals(""))
            RP.adataq = Integer.valueOf(GetString("ADATAQ"));
        if (GetString("RHSSI") != null && !GetString("RHSSI").equals(""))
            RP.rhssi = Integer.valueOf(GetString("RHSSI"));
        if (GetString("INVW") != null && !GetString("INVW").equals(""))
            RP.invw = Integer.valueOf(GetString("INVW"));
        if (GetString("ISO6BDEEP") != null
                && !GetString("ISO6BDEEP").equals(""))
            RP.iso6bdeep = Integer.valueOf(GetString("ISO6BDEEP"));
        if (GetString("ISO6BDEL") != null && !GetString("ISO6BDEL").equals(""))
            RP.iso6bdel = Integer.valueOf(GetString("ISO6BDEL"));
        if (GetString("ISO6BBLF") != null && !GetString("ISO6BBLF").equals(""))
            RP.iso6bblf = Integer.valueOf(GetString("ISO6BBLF"));

        return RP;

    }
}
