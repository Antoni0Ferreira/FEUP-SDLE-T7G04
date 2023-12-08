package utils;

import java.io.*;

public class Database {
    public static boolean writeToFile(Object inputObject, String filepath) throws IOException {
        ObjectOutputStream oos = null;
        FileOutputStream fout = null;
        try{
            fout = new FileOutputStream(filepath, true);
            oos = new ObjectOutputStream(fout);
            oos.writeObject(inputObject);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        } finally {
            if(oos != null){
                oos.close();
            }
        }
        return true;
    }

    public static Object readFromFile(String filepath) throws IOException, ClassNotFoundException {
        ObjectInputStream objectinputstream = null;
        Object object = null;
        try {
            FileInputStream streamIn = new FileInputStream(filepath);
            objectinputstream = new ObjectInputStream(streamIn);
            object = objectinputstream.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(objectinputstream != null){
                objectinputstream.close();
            }
        }
        return object;
    }

    public static boolean deleteFile(String filepath) {
        File file = new File(filepath);
        return file.delete();
    }
}
