package com.centit.support.office;

import com.centit.support.file.FileSystemOpt;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public abstract class OfficeToPdf {
	private static Log logger = LogFactory.getLog(OfficeToPdf.class);
	//private static final int ppSaveAsPDF = 32;
	/**
	 * 修改 excel多sheet 转换PDF 问题，一张表格 超大不分页，一个sheet = 一页pdf
	 * @param inputFile 输入excel文件
	 * @param pdfFile 临时pdf
	 * @return 是否成功
	 */
	public static boolean excel2Pdf(String inputFile, String pdfFile) {
		ComThread.InitSTA();
        boolean successed =true;
		ActiveXComponent actcom = new ActiveXComponent("Excel.Application");
        int count=0;
        String outFile = pdfFile.substring(0,pdfFile.lastIndexOf("."));
		try {
			actcom.setProperty("Visible", new Variant(false));
			Dispatch workbooks = actcom.getProperty("Workbooks").toDispatch();
			Dispatch excel = Dispatch.invoke(workbooks,"Open",Dispatch.Method,  
					new Object[]{inputFile,new Variant(false),new Variant(false)},  new int[9] ).toDispatch();
			Dispatch sheets= Dispatch.get(excel, "Sheets").toDispatch();
			count = Dispatch.get(sheets, "Count").getInt();

			//將每一個sheet 分開轉換成 單獨的pdf
	        for (int i = 1; i <=count ; i++) {
                try {
                    //獲得當前sheet
                    Dispatch sheet = Dispatch.invoke(sheets, "Item",
                            Dispatch.Get, new Object[]{i}, new int[1]).toDispatch();
                    //設置當前sheet 內容在一頁展示
                    Dispatch page = Dispatch.call(sheet, "PageSetup").toDispatch();
                    Dispatch.put(page, "PrintArea", false);//false或"" 表示打印sheet页中的 整个区域， 可以使用 excel表达式指定 要打印的单元格范围 ，比如 "$A$1:$C$5" 表示打印 A1-C5的单元格区域
                    Dispatch.put(page, "Orientation", 2);// 打印方向 1横向  2纵向
                    /**
                     * 将所有内容 无论行，列 有多少 都在一页显示，
                     * Zoom 必须为false ,FitToPagesTall、FitToPagesWide才有效！！
                     */
                    Dispatch.put(page, "Zoom", false);      //值为100=false， 缩放 10-400  %
                    Dispatch.put(page, "FitToPagesTall", 1);  //所有行为一页--   页高
                    Dispatch.put(page, "FitToPagesWide", 1);  //所有列为一页(1或false) --页宽

                    //將當前sheet轉換成 一個pdf
                    //String sheetname = Dispatch.get(sheet, "name").toString();

                    Dispatch.call(sheet, "Activate");
                    Dispatch.call(sheet, "Select");
                    Dispatch.invoke(excel, "SaveAs", Dispatch.Method,
                            new Object[]{outFile + "-" + i + ".pdf", new Variant(57), new Variant(false),
                                    new Variant(57), new Variant(57), new Variant(false),
                                    new Variant(true), new Variant(57), new Variant(false),
                                    new Variant(true), new Variant(false)}, new int[1]);
                    //System.out.println("Excel sheet to pdf Success :"+outFile+"-"+ i+".pdf");
                }catch (Exception e){
                    logger.error("可能是因为页面 "+i+" 没有任何元素导致打印失败！ "+e.getMessage());
                }
	        }
            //关闭
            Dispatch.call(excel, "Close", new Variant(false));
            //退出
            if (actcom != null) {
                actcom.invoke("Quit", new Variant[0]);
            }
		} catch (Exception es) {
            successed =false;
			es.printStackTrace();
		}finally {
            //释放jcom线程
            ComThread.Release();
        }

        //將多個 pdf 合併到一個pdf,可能會有 頁面大小不一問題，需要合併之前 求出最大頁面pageSizes
        if(count>0){
            try {
                File finalPdf = new File(pdfFile);//合并pdf 临时文件
                if(finalPdf.exists()) {
                    finalPdf.delete();
                }
                Document document = new Document();
                FileOutputStream out = new FileOutputStream(finalPdf);
                PdfCopy copy = new PdfCopy(document, out);
                document.open();
                for (int i = 1; i <= count; i++) {
                    File pdfPieceFile = new File(outFile + "-" + i + ".pdf");//合并pdf 临时文件
                    if (pdfPieceFile.exists()) {
                        PdfReader reader = new PdfReader(outFile + "-" + i + ".pdf");
                        int n = reader.getNumberOfPages();
                        for (int j = 1; j <= n; j++) {
                            document.newPage();
                            PdfImportedPage page = copy.getImportedPage(reader, j);
                            copy.addPage(page);
                        }
                        reader.close();
                    }
                    pdfPieceFile.delete();
                }
                copy.close();
                document.close();
                out.flush();
                out.close();
                //System.out.println("合并sheet pdf 到 "+pdfFile+" 成功！");
            }catch (Exception e) {
                successed =false;
                logger.error(e.getMessage(),e);//e.printStackTrace();
            }
        }
        return successed;

	}
 

	public static boolean ppt2Pdf(String inputFile, String pdfFile) {
        ComThread.InitSTA();
		try {
			ActiveXComponent app = new ActiveXComponent("PowerPoint.Application");

			Dispatch ppts = app.getProperty("Presentations").toDispatch();
			Dispatch ppt = Dispatch
					.call(ppts, "Open", inputFile, Boolean.valueOf(true),
                            Boolean.valueOf(true), Boolean.valueOf(false))
					.toDispatch();
			
			File f = new File(pdfFile);
			if(f.exists()){
				f.delete();
			}
			Dispatch.call(ppt, "SaveAs", pdfFile.replace('/','\\'), Integer.valueOf(32));
			Dispatch.call(ppt, "Close");
			app.invoke("Quit");
			//System.out.println("ppt转换为PDF完成！");
            return true;
		} catch (Exception e) {
			logger.error(e.getMessage(),e);//e.printStackTrace();
		}finally {
            ComThread.Release();
        }
        return false;
	}

	public static boolean word2Pdf(String inputFile, String pdfFile) {
		ActiveXComponent app = null;
		Dispatch doc = null;
        ComThread.InitSTA();
		//long start = System.currentTimeMillis();
		try {
			app = new ActiveXComponent("Word.Application");
			// 设置word不可见
			app.setProperty("Visible", new Variant(false));
			// 打开word文件
			Dispatch docs = app.getProperty("Documents").toDispatch();
			//doc = Dispatch.call(docs,  "Open" , sourceFile).toDispatch();
			doc = Dispatch.invoke(docs,"Open",Dispatch.Method,new Object[] {
					inputFile, new Variant(false),new Variant(true) }, new int[1]).toDispatch();
			//System.out.println("打开文档..." + inputFile);
			//System.out.println("转换文档到PDF..." + pdfFile);
			File tofile = new File(pdfFile);
			// System.err.println(getDocPageSize(new File(inputFile)));
			if (tofile.exists()) {
				tofile.delete();
			}
			// Dispatch.call(doc, "SaveAs",  destFile,  17);
			// 作为html格式保存到临时文件：：参数 new Variant(8)其中8表示word转html;7表示word转txt;44表示Excel转html;17表示word转成pdf。。
			Dispatch.invoke(doc, "SaveAs", Dispatch.Method, new Object[] {
					pdfFile, new Variant(17) }, new int[1]);
			//long end = System.currentTimeMillis();
			//System.out.println("转换完成..用时：" + (end - start) + "ms.");
		} catch (Exception e) {
			e.printStackTrace();
            logger.error(e.getMessage(),e);//e.printStackTrace();
			//System.out.println("========Error:文档转换失败：" + e.getMessage());
		}catch(Throwable t){
			t.printStackTrace();
		} finally {
			// 关闭word
			Dispatch.call(doc,"Close",false);
			//System.out.println("关闭文档");
			if (app != null)
				app.invoke("Quit", new Variant[] {});
		}
		//如果没有这句话,winword.exe进程将不会关闭
	    ComThread.Release();
	    return true;
	}


	public static boolean office2Pdf(String inputFile, String pdfFile) {
		String suffix = inputFile.substring(inputFile.lastIndexOf(".") + 1).toLowerCase();
		//System.out.println("文件后缀："+suffix);
		File file = new File(inputFile);
		if (!(file.exists())) {
			//System.err.println("文件不存在！");
			return false;
		}
		if (suffix.equalsIgnoreCase("pdf")) {
			//System.out.println("PDF文件无需转换为PDF!");
			try {
				FileSystemOpt.fileCopy(inputFile, pdfFile);
				return true;
			} catch (IOException e) {
			}
			return false;
		}
		if (suffix.equalsIgnoreCase("doc") || suffix.equalsIgnoreCase("docx"))
			return word2Pdf(inputFile, pdfFile);
		if (suffix.equalsIgnoreCase("ppt") || suffix.equalsIgnoreCase("pptx"))
			return ppt2Pdf(inputFile, pdfFile);
		if (suffix.equalsIgnoreCase("xls") || suffix.equalsIgnoreCase("xlsx")
                || suffix.equalsIgnoreCase("xlsm") ) {
			return excel2Pdf(inputFile, pdfFile);
		}
		//System.out.println("文件格式不支持转换为PDF!");
		return false;
	}	

}