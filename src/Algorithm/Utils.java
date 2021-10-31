package Algorithm;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {
    private static Format f = new SimpleDateFormat("HH:mm:ss");
    private Utils(){
        throw new RuntimeException("[Warning] Util Object Cannot Be Initialized");
    }
    public static String getTime(){
        return f.format(new Date());
    }

    public static void log(String title, String msg){
        System.out.println(getTime()+" ["+title+"] "+msg);
    }
}
