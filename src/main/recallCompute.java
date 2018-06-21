package main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.Buffer;

public class recallCompute {
    public static void main(String[] args){
        //通过比较两个两个文件中坐标重合的数量计算回归率。
        try {
            FileReader fr1 = new FileReader("C:\\Users\\Administrator\\IdeaProjects\\skqLSH\\result.txt");
            FileReader fr2= new FileReader("C:\\Users\\Administrator\\IdeaProjects\\skqLSH\\ideal_result.txt");
            BufferedReader br1 = new BufferedReader(fr1);
            BufferedReader br2 = new BufferedReader(fr2);

            int count = 0;
            String str1,str2;
            while((str1=br1.readLine())!=null){
                String[] s1 = str1.split("\\s");
                //System.out.println(s1[3]);
                while((str2=br2.readLine())!=null){
                    String[] s2 = str2.split("\\s");
                    if(s1[2]==s2[0]&&s1[3]==s2[1])
                        count++;
                }
            }

            System.out.println("The recall of the LSH based query is: "+count/100);
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
