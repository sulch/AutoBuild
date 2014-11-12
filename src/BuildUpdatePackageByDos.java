import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
/**
*针对zzgrid工程自动打包程序
*该工具使用须知：
*1、需要打包的文件命名为"补丁列表.txt"
*2、补丁文件与"补丁列表.txt"在同一个目录下
*3、s端的工程名称需要包含s(S)，否则需要设置projectNameS；p端的工程名称需要包含p(P)，否则需要设置projectNameP
*4、使用时，需要更改的参数有：workSpace、version和packagePath
*/
public class BuildUpdatePackageByDos {
	public static void main(String[] args) throws Exception{
		BuildUpdatePackageByDos bup = new BuildUpdatePackageByDos();
		StringBuilder sb = new StringBuilder();
		String projectPath = "";
		String applicationName = "";
		String workSpace = "F:\\RH";
		String version = "";
		String sourceDir = version + "WebRoot";//
		
		//String version2 = "zzgrid_s";//p端还是s端，如：zzgrid_p
//		String svnVersion = "67349";//svn补丁包版本号
//		String bugNum = "STORY#575";//BUG号
		
//		String packageName2 = version2+"_"+svnVersion+"_"+bugNum;//补丁包顶层名称
		String packagePath = "F:\\补丁文件";//补丁列表存放位置
//		String packagePath2 = packagePath+"\\";//更新包存放路径
		//String packagePath = "C:\\SVN\\FFCS\\IN_A_MASS\\16_每天增量补丁包\\20140624_65513";//更新包存放路径
		
		String projectNameS = "";//s端的项目名称:zzgrid_s_2.0.011
		String projectNameP = "zzgrid_p_2.0.011";//p端的项目名称:zzgrid_p_2.0.011
		
		File workSpaceDir = new File(workSpace);
		File files[] = workSpaceDir.listFiles();
		for(int i = 0;i<files.length;i++){
			System.out.println(i+1+" : "+files[i].getName());
			if("".equals(projectNameS) || "".equals(projectNameP)){
				if("".equals(projectNameS) && files[i].getName().toLowerCase().contains("s")){
					projectNameS = files[i].getName();
				}
				
				if("".equals(projectNameP) && files[i].getName().toLowerCase().contains("p")){
					projectNameP = files[i].getName();
				}
			}else{
				break;
			}
		}
		
		String svf = packagePath + "\\补丁列表.txt";//补丁列表文件存放目录
		File svnInfo = new File(svf);
		FileReader fr = new FileReader(svnInfo);
		BufferedReader br = new BufferedReader(fr);
		
		int totalCount = 0;
		int directoryCount = 0;
		int undoFileCount = 0;
		String info = br.readLine();
		while(info != null){
			if(info.contains("/")){
				info = info.replaceAll("/", "\\\\");
			}
			if("".equals(info)){//跳过空行
				info = br.readLine();
				continue;
			}
			String fileType = bup.getFileType(info).trim();//去除多余的空格
			if(fileType != null){
				String sourceFilePath = null;
				String targetPath = null;
				boolean flag = true;
				boolean s2p = false;//判断是否有对象和接口需要复制到P端
				
				if("".equals(projectNameS) && "".equals(projectNameP)){
					for(int i = 0;i<files.length;i++){//获取p端或者s端的路径
						if(info.contains(files[i].getName())){
							applicationName = files[i].getName();
							projectPath = workSpace + "\\" + applicationName;
							break;
						}
					}
				}else{
					if(info.contains(projectNameS)){
						applicationName = projectNameS;
					}else if(info.contains(projectNameP)){
						applicationName = projectNameP;
					}
					
					projectPath = workSpace + "\\" + applicationName;
				}
				
				if(fileType.equals("java")){//java文件
					info = info.substring(0,info.lastIndexOf("."))+".class";
					int pos = info.indexOf("src")+3;
					sourceFilePath = projectPath+"\\"+sourceDir+"\\WEB-INF\\classes"+info.substring(pos).replace("/", "\\");
					targetPath = packagePath+"\\"+applicationName+"\\WEB-INF\\classes"+info.substring(pos).replace("/", "\\");
					totalCount = buildInnerClass(sourceFilePath,targetPath,totalCount,sb);
					//判断是否包含bo和api目录
					if(info.contains(projectPath+"\\"+version+"bo\\") || info.contains(projectPath+"\\"+version+"api\\")){
						s2p = true;
					}
				}else if(info.indexOf("resources") != -1){//classes下的配置文件--mysql调度配置文件不输出remoting-beans.xml、zzgrid_serv_hessian.xml
					if (info.indexOf("remoting-beans.xml")!=-1 || info.indexOf("zzgrid_serv_hessian.xml")!=-1) {
						undoFileCount++;
						flag = false;
					}else {
						int pos = info.indexOf("resources")+9;
						sourceFilePath = projectPath+"\\"+sourceDir+"\\WEB-INF\\classes"+info.substring(pos).replace("/", "\\");
						targetPath = packagePath+"\\"+applicationName+"\\WEB-INF\\classes"+info.substring(pos).replace("/", "\\");
					}
				}else{//根目录下的其他文件
					int pos = info.indexOf(sourceDir);
					if(pos != -1){
						/*if(info.indexOf("login.jsp") > -1){
							appendStr = "有一个登录页面生成，请检查页面是否带有用户名或密码";
						}*/
						sourceFilePath = projectPath+"\\"+info.substring(pos).replace("/", "\\");
						targetPath = packagePath+"\\"+applicationName+info.substring(pos+sourceDir.length()).replace("/", "\\");//去掉WebRoot路径
					}
				}
				if (flag) {
					File checkFile = new File(sourceFilePath);
					if(!checkFile.isDirectory()){
						FileInputStream fis = new FileInputStream(sourceFilePath);
						String dir = targetPath.substring(0,targetPath.lastIndexOf("\\"));
						File f1 = new File(dir);
						if(!f1.exists()){
							f1.mkdirs();
						}
						FileOutputStream fos = new FileOutputStream(targetPath);
						byte b[] = new byte[1024 * 10];
						int bi = fis.read(b);
						while(bi > 0){
							fos.write(b,0,bi);
							fos.flush();
							bi = fis.read(b);
						}
						System.out.print("生成更新包文件>>>");
						System.out.println(targetPath);
						sb.append(targetPath).append("\r\n");
						totalCount++;
					}
				}else {
					String message = "警告：更新文件中包含配置文件"+info.substring(info.lastIndexOf("/")+1)+"，请手动添加配置信息。";
					System.out.println(message);
					sb.append(message).append("\r\n");
				}
				
				if (s2p) {//将s端的bo和api同时放置到p端
					if(targetPath.contains(projectNameS)){
						targetPath = targetPath.replace(projectNameS, projectNameP);
					}
					
					File checkFile = new File(sourceFilePath);
					if(!checkFile.isDirectory()){
						FileInputStream fis = new FileInputStream(sourceFilePath);
						String dir = targetPath.substring(0,targetPath.lastIndexOf("\\"));
						File f1 = new File(dir);
						if(!f1.exists()){
							f1.mkdirs();
						}
						FileOutputStream fos = new FileOutputStream(targetPath);
						byte b[] = new byte[1024 * 10];
						int bi = fis.read(b);
						while(bi > 0){
							fos.write(b,0,bi);
							fos.flush();
							bi = fis.read(b);
						}
						System.out.print("生成更新包文件>>>");
						System.out.println(targetPath);
						sb.append(targetPath).append("\r\n");
						totalCount++;
					}
				}
				
			}else{
				directoryCount++;
			}
			info = br.readLine();
		}
		FileWriter fw = new FileWriter(packagePath+"\\生成文件清单.txt");
		fw.write(sb.toString());
		fw.close();
		System.out.print("共生成了 ["+totalCount+"]个文件,有 ["+directoryCount+"]个文件为文件夹目录被跳过,有["+undoFileCount+"]个配置文件被跳过");
	}
	
	private static int buildInnerClass(String sourceFilePath,String targetPath,int totalCount,StringBuilder sb) throws Exception{
		String sourceDirPath = sourceFilePath.substring(0,sourceFilePath.lastIndexOf("\\"));
		String targetDirPath = targetPath.substring(0,targetPath.lastIndexOf("\\"));
		File[] files = new File(sourceDirPath).listFiles();
		String fileName = sourceFilePath.substring(sourceFilePath.lastIndexOf("\\")+1);
		fileName = fileName.substring(0,fileName.lastIndexOf("."));
		if(files!=null){
			for(File f:files){
				String name = f.getName();
				if(name.startsWith(fileName+"$")){//含有$的class文件
					FileInputStream fis = new FileInputStream(sourceDirPath+"\\"+name);
					File f1 = new File(targetDirPath);
					if(!f1.exists()){
						f1.mkdirs();
					}
					targetPath = targetDirPath+ "\\"+name;
					FileOutputStream fos = new FileOutputStream(targetPath);
					sb.append(targetPath).append("\r\n");
					byte b[] = new byte[1024 * 10];
					int bi = fis.read(b);
					while(bi > 0){
						fos.write(b,0,bi);
						fos.flush();
						bi = fis.read(b);
					}
					System.out.print("生成更新包文件>>>");
					System.out.println(targetPath);
					sb.append(targetPath).append("\r\n");
					totalCount++;
				}
			}
		}else {
			System.err.println("文件目录不存在：\n"+sourceDirPath);
		}
		return totalCount;
	}
	private String getFileType(String fileName){
		int pos = fileName.lastIndexOf(".");
		if(pos == -1){
			return null;
		}
		return fileName.substring(pos+1);
	}
}