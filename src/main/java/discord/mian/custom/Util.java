package discord.mian.custom;

import java.sql.Connection;
import java.sql.DriverManager;

public class Util {
    public static String botifyMessage(String string){
        return "```" + string + "```";
    }
    public static Connection openDatabase(String dbName){
        Connection c = null;

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:"+dbName+".db");
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println("Opened database successfully");

        return c;
    }
}
