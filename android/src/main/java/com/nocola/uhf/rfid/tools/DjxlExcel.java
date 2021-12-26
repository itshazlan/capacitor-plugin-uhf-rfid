package com.nocola.uhf.rfid.tools;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import jxl.Workbook;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.Colour;
import jxl.format.Orientation;
import jxl.format.UnderlineStyle;
import jxl.format.VerticalAlignment;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

public class DjxlExcel {

  String fullfilename;
  WritableWorkbook wworkbook;
  String befstr = "A0000000000000000000";
  int X, Y;
  WritableSheet Sheet1;

  public DjxlExcel(String filename) {

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
      "yyyyMMdd_HHmmss");// HH:mm:ss
    // 获取当前时间
    Date date = new Date(System.currentTimeMillis());

    fullfilename = "/sdcard/ARFID";
    boolean sdCardExist = Environment.getExternalStorageState().equals(
      android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
    if (sdCardExist) {
      // fullfilename =
      // Environment.getExternalStorageDirectory().getPath()+
      // File.separator + "ARFID" + File.separator;//获取跟目录
      fullfilename = Environment.getExternalStorageDirectory().getPath()
        + File.separator;
    }

    File file = new File(fullfilename);
    if (!file.exists())
      file.mkdir();

    fullfilename += filename + "_" + simpleDateFormat.format(date) + ".xls";

  }

  /***
   *
   * @param sheet1
   * @param x
   *            x是矩阵的x轴长度
   * @param y
   *            y是矩阵y轴的下行号
   * @param row
   *            y轴上行号
   * @param sort
   * @param wcf_table_green
   * @param wcf_table_red
   * @param ltags
   * @return
   * @throws RowsExceededException
   * @throws WriteException
   */
  private int square_v1(WritableSheet sheet1, int x, int y, int row,
                        int sort, WritableCellFormat wcf_table_green,
                        WritableCellFormat wcf_table_red, List<String> ltags)
    throws RowsExceededException, WriteException {
    for (int i = 0; i < x; i++) {
      for (int j = row; j < y; j++) {
        int val = sort + (i) * (y - row) + (j - row) + 1;

        String valstr = befstr + String.format("%04d", val);
        if (ltags.contains(valstr))
          sheet1.addCell(new Label(i, j, valstr, wcf_table_green));
        else
          sheet1.addCell(new Label(i, j, valstr, wcf_table_red));

        if (i == x - 1 && j == y - 1)
          sort = val;
      }
    }

    return sort;
  }

  /***
   *
   * @param sheet1
   * @param x
   *            x是矩阵的x轴长度
   * @param y
   *            y是矩阵y轴的下行号
   * @param row
   *            y轴上行号
   * @param sort
   * @param wcf_table_green
   * @param wcf_table_red
   * @param ltags
   * @return
   * @throws RowsExceededException
   * @throws WriteException
   */
  private int rectangular_v1(WritableSheet sheet1, int x, int y, int row,
                             int sort, WritableCellFormat wcf_table_green,
                             WritableCellFormat wcf_table_red, List<String> ltags)
    throws RowsExceededException, WriteException {
    for (int j = y; j > row; j--) {
      for (int i = 0; i < x; i++) {

        int val = sort + (y - j) * x + i + 1;
        String valstr = befstr + String.format("%04d", val);
        if (ltags.contains(valstr))
          sheet1.addCell(new Label(i, j, valstr, wcf_table_green));
        else
          sheet1.addCell(new Label(i, j, valstr, wcf_table_red));

        if (i == x - 1 && j == row + 1)
          sort = val;
      }
    }

    x += 12;
    for (int j = y; j > row; j--) {
      for (int i = 12; i < x; i++) {

        int val = sort + (y - j) * (x - 12) + (i - 12) + 1;
        String valstr = befstr + String.format("%04d", val);
        if (ltags.contains(valstr))
          sheet1.addCell(new Label(i, j, valstr, wcf_table_green));
        else
          sheet1.addCell(new Label(i, j, valstr, wcf_table_red));

        if (i == x - 1 && j == row + 1)
          sort = val;
      }
    }

    return sort;
  }

  public void CreateTestBoxExcelfile_v1(List<String> ltags)
    throws IOException, WriteException {
    // FileOutputStream nfile=new FileOutputStream(fullfilename);
    File nfile = new File(fullfilename);
    // nfile.createNewFile();

    wworkbook = Workbook.createWorkbook(nfile);

    WritableSheet sheet1 = wworkbook.createSheet("sheet1", 0);
    WritableSheet sheet2 = wworkbook.createSheet("sheet2", 1);
    WritableSheet sheet3 = wworkbook.createSheet("sheet3", 2);

    WritableFont wf_table = new WritableFont(WritableFont.ARIAL, 10,
      WritableFont.NO_BOLD, false, UnderlineStyle.NO_UNDERLINE,
      jxl.format.Colour.BLACK);// 定义格式 字体 下划线 斜体 粗体 颜色

    WritableCellFormat wcf_table_green = new WritableCellFormat(wf_table);// 单元格定义
    wcf_table_green.setBackground(jxl.format.Colour.GREEN);// 设置单元格的背景颜色
    wcf_table_green.setAlignment(jxl.format.Alignment.CENTRE);// 设置对齐方式

    WritableCellFormat wcf_table_red = new WritableCellFormat(wf_table);// 单元格定义
    wcf_table_red.setBackground(jxl.format.Colour.RED);// 设置单元格的背景颜色
    wcf_table_red.setAlignment(jxl.format.Alignment.CENTRE);// 设置对齐方式
    // **
    // *使用样式的单元格
    // *
    // 1.添加Label对象三个参数意思:【列，行，值】
    // sheet1.addCell(new Label(0,0,"A0000001",wcf_table_green));
    // //普通的带有定义格式的单元格
    // sheet1.addCell(new Label(0,1,"A0000002",wcf_table_red));
    // //普通的带有定义格式的单元格
    // sheet1.mergeCells(0, 0, 5, 0); // 合并单元格

    int row = 0;
    int sort = 0;
    int x = 8, y = 9 + row;

    sort = square_v1(sheet1, x, y, row, sort, wcf_table_green,
      wcf_table_red, ltags);

    for (int k = 0; k < 10; k++) {
      row += 9;
      x = 12;
      y = 3 + row;

      sort = rectangular_v1(sheet1, x, y, row, sort, wcf_table_green,
        wcf_table_red, ltags);

      row += 3 + 2;
      x = 8;
      y = 9 + row;

      sort = square_v1(sheet1, x, y, row, sort, wcf_table_green,
        wcf_table_red, ltags);
    }

    // 写入Exel工作表
    wworkbook.write();
    // 关闭Excel工作薄对象
    wworkbook.close();
  }

  /***
   *
   * @param sheet1
   * @param x
   *            列宽 x是矩阵的x轴长度
   * @param y
   *            y是矩阵y轴的下行号
   * @param row
   *            y轴上行号
   * @param sort
   * @param wcf_table_green
   * @param wcf_table_red
   * @param ltags
   * @return
   * @throws RowsExceededException
   * @throws WriteException
   */
  private int square_v2(WritableSheet sheet1, int x, int y, int row,
                        int sort, WritableCellFormat wcf_table_green,
                        WritableCellFormat wcf_table_red, List<String> ltags)
    throws RowsExceededException, WriteException {
    for (int i = 0; i < x; i++) {
      for (int j = row; j < y; j++) {
        int val = sort + (i) * (y - row) + (j - row) + 1;

        String valstr = befstr + String.format("%04d", val);
        if (ltags.contains(valstr))
          // sheet1.addCell(new
          // Label(i,j,String.valueOf(val),wcf_table_green));
          sheet1.addCell(new Number(i, j, val, wcf_table_green));
        else
          // sheet1.addCell(new
          // Label(i,j,String.valueOf(val),wcf_table_red));
          sheet1.addCell(new Number(i, j, val, wcf_table_red));
        if (i == x - 1 && j == y - 1)
          sort = val;
      }
    }

    return sort;
  }

  /***
   *
   * @param sheet1
   * @param x
   *            x是矩阵的x轴长度
   * @param row
   *            y轴上行号
   * @param sort
   * @param wcf_table_green
   * @param wcf_table_red
   * @param ltags
   * @return
   * @throws RowsExceededException
   * @throws WriteException
   */
  private int rectangular_v2(WritableSheet sheet1, int x, int row, int sort,
                             WritableCellFormat wcf_table_green,
                             WritableCellFormat wcf_table_red, List<String> ltags)
    throws RowsExceededException, WriteException {
    int x1 = 8, x2 = 12;
    int y = 3;
    for (int j = row + y; j > row; j--) {
      for (int i = x1; i < x + x1; i++) {

        int val = sort + (row + y - j) * x + i - x1 + 1;

        // 合并
        int m2 = row + (j - row) * y - 3;

        sheet1.mergeCells(i, m2, i, m2 + 2);

        String valstr = befstr + String.format("%04d", val);
        if (ltags.contains(valstr))
          // sheet1.addCell(new
          // Label(i,m2,String.valueOf(val),wcf_table_green)
          sheet1.addCell(new Number(i, m2, val, wcf_table_green));
        else
          // sheet1.addCell(new
          // Label(i,m2,String.valueOf(val),wcf_table_red));
          sheet1.addCell(new Number(i, m2, val, wcf_table_red));

        if (i == x + x1 - 1 && j == row + 1)
          sort = val;
      }
    }

    x += x2 + x1;
    for (int j = row + y; j > row; j--) {
      for (int i = x1 + x2; i < x; i++) {

        int val = sort + (row + y - j) * (x - x1 - x2) + (i - x1 - x2)
          + 1;
        // 合并
        int m2 = row + (j - row) * y - 3;

        sheet1.mergeCells(i, m2, i, m2 + 2);

        String valstr = befstr + String.format("%04d", val);
        if (ltags.contains(valstr))
          // sheet1.addCell(new
          // Label(i,m2,String.valueOf(val),wcf_table_green));
          sheet1.addCell(new Number(i, m2, val, wcf_table_green));
        else
          // sheet1.addCell(new
          // Label(i,m2,String.valueOf(val),wcf_table_red));
          sheet1.addCell(new Number(i, m2, val, wcf_table_red));

        if (i == x - 1 && j == row + 1)
          sort = val;
      }
    }

    return sort;
  }

  public void CreateTestBoxExcelfile_v2(List<String> ltags)
    throws IOException, WriteException {
    // FileOutputStream nfile=new FileOutputStream(fullfilename);
    File nfile = new File(fullfilename);
    // nfile.createNewFile();

    wworkbook = Workbook.createWorkbook(nfile);

    WritableSheet sheet1 = wworkbook.createSheet("sheet1", 0);
    WritableSheet sheet2 = wworkbook.createSheet("sheet2", 1);
    WritableSheet sheet3 = wworkbook.createSheet("sheet3", 2);

    int rowhigh = 0;
    int font_l = 0;
    int font_r = 0;
    int col_l = 0;
    int col_r = 0;
    boolean isVprintf = true;

    if (isVprintf) {
      // 竖起打印
      rowhigh = 215;
      font_l = 7;
      font_r = 6;
      col_l = 4;
      col_r = 2;
    } else {
      font_l = 10;
      font_r = 7;
      col_l = 7;
      col_r = 3;
    }

    if (isVprintf) {
      for (int i = 0; i < 141; i++)
        sheet1.setRowView(i, rowhigh, false); // 设置行高
    }

    WritableFont wf_table = new WritableFont(WritableFont.ARIAL, font_l,
      WritableFont.NO_BOLD, false, UnderlineStyle.NO_UNDERLINE,
      jxl.format.Colour.BLACK);// 定义格式 字体 下划线 斜体 粗体 颜色

    WritableFont wf_table_s = new WritableFont(WritableFont.ARIAL, font_r,
      WritableFont.NO_BOLD, false, UnderlineStyle.NO_UNDERLINE,
      jxl.format.Colour.BLACK);// 定义格式 字体 下划线 斜体 粗体 颜色

    WritableCellFormat wcf_table_l = new WritableCellFormat(wf_table);// 单元格定义
    wcf_table_l.setVerticalAlignment(VerticalAlignment.CENTRE);
    wcf_table_l.setAlignment(Alignment.CENTRE);// 设置对齐方式
    wcf_table_l.setBorder(Border.ALL, BorderLineStyle.THIN);

    WritableCellFormat wcf_table_l_no = new WritableCellFormat(wf_table);// 单元格定义

    // Color color = Color.decode("#FAEBD7"); // 自定义的颜色

    wworkbook.setColourRGB(Colour.RED, 250, 235, 215);
    wcf_table_l_no.setBackground(Colour.RED);// 设置单元格的背景颜色
    wcf_table_l_no.setVerticalAlignment(VerticalAlignment.CENTRE);
    wcf_table_l_no.setAlignment(Alignment.CENTRE);// 设置对齐方式
    wcf_table_l_no.setBorder(Border.ALL, BorderLineStyle.THIN);

    WritableCellFormat wcf_table_r = new WritableCellFormat(wf_table_s);// 单元格定义
    wcf_table_r.setAlignment(Alignment.CENTRE);// 设置对齐方式
    wcf_table_r.setVerticalAlignment(VerticalAlignment.CENTRE);
    wcf_table_r.setOrientation(Orientation.VERTICAL);
    wcf_table_r.setBorder(Border.ALL, BorderLineStyle.THIN);

    WritableCellFormat wcf_table_r_no = new WritableCellFormat(wf_table_s);// 单元格定义
    wcf_table_r_no.setBackground(Colour.RED);// 设置单元格的背景颜色
    wcf_table_r_no.setAlignment(Alignment.CENTRE);// 设置对齐方式
    wcf_table_r_no.setVerticalAlignment(VerticalAlignment.CENTRE);
    wcf_table_r_no.setOrientation(Orientation.VERTICAL);

    wcf_table_r_no.setBorder(Border.ALL, BorderLineStyle.THIN);

    int col = 0;
    int row = 0;
    int page = 1;
    int sort = 0;
    // 设置列宽
    for (int i = 0; i < 8; i++)
      sheet1.setColumnView(i, col_l); // 设置col显示样式

    for (int i = 8; i < 32; i++)
      sheet1.setColumnView(i, col_r); // 设置col显示样式

    sheet1.mergeCells(col, row, col + 32 - 1, row); // 合并单元格
    Label lbh = new Label(0, 0, "", wcf_table_l);

    WritableCellFormat cellFormat2 = new WritableCellFormat();
    cellFormat2.setBorder(Border.ALL, BorderLineStyle.THIN);
    lbh.setCellFormat(cellFormat2);
    sheet1.addCell(lbh); // 普通的带有定义格式的单元格

    row++;

    int x = 8;// 列宽
    int y = 9;// 行数
    int x1 = 12;// 列宽
    for (int k = 0; k < 11; k++) {
      sheet1.mergeCells(col, row, col + 8 - 1, row); // 合并单元格

      Label lbl = new Label(col, row, "第" + String.valueOf(page++) + "层",
        wcf_table_l);
      // lbl.setCellFormat(cellFormat2);
      sheet1.addCell(lbl); // 普通的带有定义格式的单元格

      if (k < 10) {
        sheet1.mergeCells(col + 8, row, col + 32 - 1, row); // 合并单元格
        Label lbr = new Label(col + 8, row, "第"
          + String.valueOf(page++) + "层", wcf_table_l);
        // lbr.setCellFormat(cellFormat2);
        sheet1.addCell(lbr);
      }

      row++;

      sort = square_v2(sheet1, x, row + y, row, sort, wcf_table_l,
        wcf_table_l_no, ltags);

      if (k < 10)
        sort = rectangular_v2(sheet1, x1, row, sort, wcf_table_r,
          wcf_table_r_no, ltags);

      if (isVprintf)
        row += 9 + 1;
      else
        row += 9 + 2;

    }
    // 写入Exel工作表
    wworkbook.write();
    // 关闭Excel工作薄对象
    wworkbook.close();
  }

  public void CreatereturnlossExcelfile(List<String[]> listr)
    throws IOException, WriteException {
    File nfile = new File(fullfilename);
    // nfile.createNewFile();

    wworkbook = Workbook.createWorkbook(nfile);

    WritableSheet sheet1 = wworkbook.createSheet("sheet1", 0);
    WritableSheet sheet2 = wworkbook.createSheet("sheet2", 1);
    WritableSheet sheet3 = wworkbook.createSheet("sheet3", 2);

    WritableFont wf_table = new WritableFont(WritableFont.ARIAL, 10,
      WritableFont.NO_BOLD, false, UnderlineStyle.NO_UNDERLINE,
      jxl.format.Colour.BLACK);// 定义格式 字体 下划线 斜体 粗体 颜色

    WritableCellFormat wcf_table_green = new WritableCellFormat(wf_table);// 单元格定义
    // wcf_table_green.setBackground(jxl.format.Colour.GREEN);//设置单元格的背景颜色
    wcf_table_green.setAlignment(jxl.format.Alignment.CENTRE);// 设置对齐方式

    WritableCellFormat wcf_table_red = new WritableCellFormat(wf_table);// 单元格定义
    wcf_table_red.setBackground(jxl.format.Colour.RED);// 设置单元格的背景颜色
    wcf_table_red.setAlignment(jxl.format.Alignment.CENTRE);// 设置对齐方式

    int row = 0;
    int sort = 0;
    int x = 0, y = 0;

    sheet1.addCell(new Label(x++, y, "频率", wcf_table_green));
    sheet1.addCell(new Label(x++, y, "RL(dB)", wcf_table_green));
    sheet1.addCell(new Label(x++, y, "VRWR", wcf_table_green));
    sheet1.addCell(new Label(x++, y, "天线", wcf_table_green));

    for (int i = 0; i < listr.size(); i++) {
      x = 0;
      y++;
      String[] stray = listr.get(i);
      for (int j = 0; j < stray.length; j++) {
        sheet1.addCell(new Label(x++, y, stray[j], wcf_table_green));
      }
    }

    // 写入Exel工作表
    wworkbook.write();
    // 关闭Excel工作薄对象
    wworkbook.close();
  }

  public void CreatereExcelfile(List<String> headlistr) throws IOException,
    WriteException {
    File nfile = new File(fullfilename);
    // nfile.createNewFile();

    wworkbook = Workbook.createWorkbook(nfile);
    X = Y = 0;

    Sheet1 = wworkbook.createSheet("sheet1", 0);

    WritableFont wf_table = new WritableFont(WritableFont.ARIAL, 10,
      WritableFont.NO_BOLD, false, UnderlineStyle.NO_UNDERLINE,
      jxl.format.Colour.BLACK);// 定义格式 字体 下划线 斜体 粗体 颜色

    WritableCellFormat wcf_table_green = new WritableCellFormat(wf_table);// 单元格定义
    // wcf_table_green.setBackground(jxl.format.Colour.GREEN);//设置单元格的背景颜色
    wcf_table_green.setAlignment(jxl.format.Alignment.CENTRE);// 设置对齐方式

    int row = 0;
    int sort = 0;
    int x = 0, y = 0;

    for (int i = 0; i < headlistr.size(); i++)
      Sheet1.addCell(new Label(x++, y, headlistr.get(i), wcf_table_green));
    Y++;

    // 写入Exel工作表
    // wworkbook.write();

  }

  public void CreatereExcelfile(String head, List<String> headlistr)
    throws IOException, WriteException {
    File nfile = new File(fullfilename);
    // nfile.createNewFile();

    wworkbook = Workbook.createWorkbook(nfile);
    X = Y = 0;

    Sheet1 = wworkbook.createSheet("sheet1", 0);

    WritableFont wf_table = new WritableFont(WritableFont.ARIAL, 10,
      WritableFont.NO_BOLD, false, UnderlineStyle.NO_UNDERLINE,
      jxl.format.Colour.BLACK);// 定义格式 字体 下划线 斜体 粗体 颜色

    WritableCellFormat wcf_table_green = new WritableCellFormat(wf_table);// 单元格定义
    // wcf_table_green.setBackground(jxl.format.Colour.GREEN);//设置单元格的背景颜色
    wcf_table_green.setAlignment(jxl.format.Alignment.CENTRE);// 设置对齐方式

    int row = 0;
    int sort = 0;
    int x = 0, y = 0;

    Sheet1.mergeCells(0, 0, headlistr.size(), 0);

    Label lbh = new Label(0, Y, head, wcf_table_green);
    Sheet1.addCell(lbh);
    Y++;
    for (int i = 0; i < headlistr.size(); i++)
      Sheet1.addCell(new Label(x++, Y, headlistr.get(i), wcf_table_green));
    Y++;

    // 写入Exel工作表
    // wworkbook.write();

  }

  public void WriteExcelfile(Object[] objes) throws IOException,
    WriteException {

    WritableFont wf_table = new WritableFont(WritableFont.ARIAL, 10,
      WritableFont.NO_BOLD, false, UnderlineStyle.NO_UNDERLINE,
      jxl.format.Colour.BLACK);// 定义格式 字体 下划线 斜体 粗体 颜色

    WritableCellFormat wcf_table_green = new WritableCellFormat(wf_table);// 单元格定义
    // wcf_table_green.setBackground(jxl.format.Colour.GREEN);//设置单元格的背景颜色
    wcf_table_green.setAlignment(jxl.format.Alignment.CENTRE);// 设置对齐方式
    int x = 0;
    for (int i = 0; i < objes.length; i++) {
      if (objes[i] instanceof String)
        Sheet1.addCell(new Label(x++, Y, (String) objes[i],
          wcf_table_green));
      else if (objes[i] instanceof Integer)
        Sheet1.addCell(new Number(x++, Y, (int) (Integer) objes[i],
          wcf_table_green));
      else if (objes[i] instanceof Long)
        Sheet1.addCell(new Number(x++, Y, (Long) objes[i],
          wcf_table_green));
      else if (objes[i] instanceof Float)
        Sheet1.addCell(new Number(x++, Y, (Float) objes[i],
          wcf_table_green));
      else if (objes[i] instanceof Double)
        Sheet1.addCell(new Number(x++, Y, (Double) objes[i],
          wcf_table_green));
    }
    Y++;

  }

  public void Addline(int l) {
    Y += l;
  }

  public int GetY() {
    return Y;
  }

  public void mergeandtext(int x1, int y1, int x2, int y2, String msg) {
    try {
      Sheet1.mergeCells(x1, y1, x2, y2);
      WritableFont wf_table = new WritableFont(WritableFont.ARIAL, 10,
        WritableFont.NO_BOLD, false, UnderlineStyle.NO_UNDERLINE,
        jxl.format.Colour.BLACK);// 定义格式 字体 下划线 斜体 粗体 颜色

      WritableCellFormat wcf_table_l = new WritableCellFormat(wf_table);// 单元格定义
      wcf_table_l.setVerticalAlignment(VerticalAlignment.CENTRE);
      wcf_table_l.setAlignment(Alignment.CENTRE);// 设置对齐方式
      Label lbh = new Label(x1, Y, msg, wcf_table_l);
      Sheet1.addCell(lbh);
    } catch (RowsExceededException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (WriteException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } // 合并单元格
  }

  public void SaveandCloseExcelfile() throws IOException, WriteException {
    // 写入Exel工作表
    wworkbook.write();
    // 关闭Excel工作薄对象
    wworkbook.close();
  }

}
