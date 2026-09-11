import java.util.ArrayList;
import java.util.List;

public class DataLoader {
    static List<Player> loadPlayers(String content) {
        int stop1 = content.indexOf("\"events\"");          // 锁 players 段: 终点=events 键起点,防跨段串扰
        List<Player> list = new ArrayList<>();
        int from = 0;
        while (true) {
            int k = content.indexOf("\"fullName\": \"", from);
            if (k == -1 || k >= stop1) break;
            // 从 k 抠出 fullName
            int startName = k + "\"fullName\": \"".length();
            int endName = content.indexOf("\"", startName);
            String fullName = content.substring(startName, endName);

            // 抠 gender —— content.indexOf("\"gender\": \"", k) 从 k 往后找
            int genderIdx = content.indexOf("\"gender\": \"", endName);
            int startGender = genderIdx + "\"gender\": \"".length();
            int endGender = content.indexOf("\"", startGender);
            String gender = content.substring(startGender, endGender);

            // 抠 country
            int countryIdx = content.indexOf("\"country\": \"", endGender);
            int startCountry = countryIdx + "\"country\": \"".length();
            int endCountry = content.indexOf("\"", startCountry);
            String country = content.substring(startCountry, endCountry);

            // new Player(...) 塞进 list
            list.add(new Player(fullName, gender, country));
            // from 更新为上一轮值结束的位置,继续下一轮
            from = endCountry + 1;
            }
            return list;
        }

    static List<String[]> loadResults(String dataContent, String command){

        List<String[]> list = new ArrayList<>();
        int k = DataLoader.findEvent(dataContent, command);   // 把 k 存下来
        if (k == -1) return list;
        int eventEnd = dataContent.indexOf("\"command\": ", k + 1);   // k+1:跳过自己,从command后面找下一个
        if (eventEnd == -1) eventEnd = dataContent.length();          // 最后一个event:到文件尾

        int temp = k;

        while (true) {
            int nameIdx = dataContent.indexOf("\"fullName\": \"", temp);
            if (nameIdx == -1 || nameIdx >= eventEnd) break;   // 出了本event范围,收工
            int nameStart = nameIdx + "\"fullName\": \"".length();
            int nameEnd = dataContent.indexOf("\"", nameStart);
            String fullName = dataContent.substring(nameStart, nameEnd);

            // 抠 rank: 从 nameEnd 往后找 "rank":
            int rankIdx = dataContent.indexOf("\"rank\": ", nameEnd);
            int rankStart = rankIdx + "\"rank\": ".length();
            int rankEnd = dataContent.indexOf(",", rankStart);          // 数字后紧跟逗号
            String rank = dataContent.substring(rankStart, rankEnd).trim();

            // 抠 scores: 先找 "scores": [ ，再在 [ 和 ] 之间反复抠引号串
            int scoresIdx = dataContent.indexOf("\"scores\": [", rankEnd);
            int cursor = scoresIdx + "\"scores\": [".length();
            int scoresClose = dataContent.indexOf("]", cursor);
            List<String> scores = new ArrayList<>();
            while (cursor < scoresClose) {                              // 交替配对,看懂这行
                int q1 = dataContent.indexOf("\"", cursor);              // 找开引号
                if (q1 == -1 || q1 >= scoresClose) break;
                int q2 = dataContent.indexOf("\"", q1 + 1);              // 找闭引号
                scores.add(dataContent.substring(q1 + 1, q2));
                cursor = q2 + 1;
            }

            // 抠 totalPoints
            int totalIdx = dataContent.indexOf("\"totalPoints\": \"", scoresClose);
            int totalStart = totalIdx + "\"totalPoints\": \"".length();
            int totalEnd = dataContent.indexOf("\"", totalStart);
            String totalPoints = dataContent.substring(totalStart, totalEnd);


            list.add(new String[]{fullName, rank,String.join(",", scores) ,totalPoints});  // String.join 便于格式化层 split 回来
            temp = totalEnd + 1;        // 游标跳过整条,进入下一条 result
        }
        return list;
    }

    // 查找事件
    static int findEvent(String content, String command) {
        String key = "\"command\": \"";
        int from = content.indexOf("\"events\"");        // 只在 events 段搜,players 段没有 command 也无所谓,但养成锁段习惯
        while (from >= 0) {
            int k = content.indexOf(key, from);
            if (k == -1) break;
            int start = k + key.length();                 // 键和长度必须同一个字符串
            int end   = content.indexOf("\"", start);
            if (end == -1) break;
            String value = content.substring(start, end);
            if (value.equals(command)) return k;          // 找到了,返回 event 位置
            from = end + 1;
        }
        return -1;
    }
}
