package io.github.theminiluca.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

@Deprecated
public class Rankboard {
    private final String tableName;
    private final SQLSyncManager sqlSyncManager;

    public Rankboard(String tableName, SQLSyncManager sqlSyncManager) {
        this.tableName = tableName;
        this.sqlSyncManager = sqlSyncManager;
        try {
            sqlSyncManager.sqlManager.createRankboardTable(tableName);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Rankboard에다가 랭크를 저장합니다.
     *
     * @param uniqueValue 플레이어 UUID
     * @param score 플레이어 스코어
     * */
    public void setRank(String uniqueValue, int score) {
        try {
            ResultSet set = sqlSyncManager.sqlManager.executeF("SELECT 1 FROM `%s` WHERE `uniqueValue`='%s';", tableName, uniqueValue);
            if(set.first()) {
                sqlSyncManager.sqlManager.executeF("UPDATE `%s` SET `score`=%s WHERE `uniqueValue`='%s'", tableName, score, uniqueValue);
            }else {
                sqlSyncManager.sqlManager.executeF("INSERT INTO `%s` (`uniqueValue`, `score`) VALUES ('%s', %s);", tableName, uniqueValue, score);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Rankboard에서 해당 플래이어가 몇번째 인지 불러옵니다.
     *
     * @param value 플레이어 UUID
     * */
    public int getPositionOf(String value) {
        try {
            ResultSet set = sqlSyncManager.sqlManager.executeF("""
                    SELECT RowNr
                    FROM (
                        SELECT\s
                             ROW_NUMBER() OVER (ORDER BY score desc) AS RowNr,
                             uniqueValue
                        FROM %s
                    ) sub
                    WHERE sub.uniqueValue = "%s";
                    """, tableName, value);
            if(set.first()) return set.getInt("RowNr");
            else return -1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Rankboard에서 해당 플래이어의 점수를 불러옵니다.
     *
     * @param value 플레이어 UUID
     * */
    public int getScoreOf(String value) {
        try {
            ResultSet set = sqlSyncManager.sqlManager.executeF("SELECT `score` FROM %s WHERE `uniqueValue`=\"%s\";", tableName, value);
            if(set.first()) return set.getInt("score");
            else return 0;
        }catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 상위 `top`를 가져옵니다.
     * @param top 상위 xx
     * */
    public RankValue[] getTop(int top) {
        return getUniqueValuesInRange(1, top+1);
    }

    /**
     * `from` 부터 `to`까지 순위에 있는 값을 가져옵니다.
     * */
    public RankValue[] getUniqueValuesInRange(int from, int to) {
        try {
            if(to-from < 0) {
                throw new RuntimeException("from(%s) is bigger than to(%s)".formatted(from, to));
            }
            ResultSet set = sqlSyncManager.sqlManager.executeF("""
                    SELECT RowNr, uniqueValue, score
                    FROM (
                        SELECT\s
                             ROW_NUMBER() OVER (ORDER BY score desc) AS RowNr,
                             uniqueValue, score
                        FROM %s
                    ) sub
                    WHERE RowNr BETWEEN %s AND %S
                    """, tableName, from, to-1);
            long updated = System.currentTimeMillis();
            RankValue[] list = new RankValue[to-from];
            int i = 0;
            while (set.next()) {
                list[i] = new RankValue(set.getString("uniqueValue"), set.getInt("score"), set.getInt("RowNr"), updated);
                i++;
            }
            return list;
        }catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static class RankValue {
        private final String uniqueValue;
        private final int score;
        private final int num;
        private final long updated;
        private RankValue(String uniqueValue, int score, int num, long updated) {
            this.uniqueValue = uniqueValue;
            this.score = score;
            this.num = num;
            this.updated = updated;
        }

        /**
         * 스코어가 value 보다 큰지 비교합니다.
         *
         * @param value 비교 대상
         * */
        public boolean isScoreBiggerThan(RankValue value) {
            return (score - value.score) > 0;
        }

        /**
         * 플레이어 UUID를 가져옵니다.
         * */
        public String getUniqueValue() {
            return uniqueValue;
        }

        /**
         * 스코어를 가져옵니다.
         * */
        public int getScore() {
            return score;
        }

        /**
         * 이 랭크를 가져올 당시에, 순위를 가져옵니다.
         * */
        public int getNum() {
            return num;
        }

        /**
         * 언제 업데이트 되었는지를 ms로 가져옵니다.
         * */
        public long getUpdated() {
            return updated;
        }
    }
}
