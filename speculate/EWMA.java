package org.apache.hadoop.mapreduce.v2.app.speculate;

import java.io.Serializable;
 
public class EWMA extends StartEndTimesBase implements Serializable {
 
    private static final long serialVersionUID = 2979391326784043002L;
     
    //时间类型枚举
    public static enum Time {
        MILLISECONDS(1), SECONDS(1000), MINUTES(SECONDS.getTime() * 60), HOURS(MINUTES.getTime() * 60), DAYS(HOURS.getTime() * 24), WEEKS(DAYS.getTime() * 7);
         
        private long millis;
 
        private Time(long millis) {
            this.millis = millis;
        }
 
        public long getTime() {
            return this.millis;
        }
    }
    //三个alpha常量，这些值和Unix系统计算负载时使用的标准alpha值相同
    public static final double ONE_MINUTE_ALPHA = 1 - Math.exp(-5d / 60d / 1d);
    public static final double FIVE_MINUTE_ALPHA = 1 - Math.exp(-5d / 60d / 5d);
    public static final double FIFTEEN_MINUTE_ALPHA = 1 - Math.exp(-5d / 60d / 15d);
    private long window;
    private long alphaWindow;
    private long last;
    private double average;
    private double alpha = -1D;
    private boolean sliding = false;
 
    public EWMA() {
    }
 
    public EWMA sliding(double count, Time time) {
        return this.sliding((long) (time.getTime() * count));
    }
 
    public EWMA sliding(long window) {
        this.sliding = true;
        this.window = window;
        return this;
    }
 
    public EWMA withAlpha(double alpha) {
        if (!(alpha > 0.0D && alpha <= 1.0D)) {
            throw new IllegalArgumentException("Alpha must be between 0.0 and 1.0");
        }
        this.alpha = alpha;
        return this;
    }
 
    public EWMA withAlphaWindow(long alphaWindow) {
        this.alpha = -1;
        this.alphaWindow = alphaWindow;
        return this;
    }
 
    public EWMA withAlphaWindow(double count, Time time) {
        return this.withAlphaWindow((long) (time.getTime() * count));
    }
     
    //没有参数的话，当前时间来计算平均值
    public void mark() {
        mark(System.currentTimeMillis());
    }
 
    //用来更新移动平均值，没有参数的话，使用当前时间来计算平均值
    public synchronized void mark(long time) {
        if (this.sliding) {
            if (time - this.last > this.window) {
                this.last = 0;
            }
        }
        if (this.last == 0) {
            this.average = 0;
            this.last = time;
        }
        long diff = time - this.last;
        double alpha = this.alpha != -1.0 ? this.alpha : Math.exp(-1.0 * ((double) diff / this.alphaWindow));
        this.average = (1.0 - alpha) * diff + alpha * this.average;
        this.last = time;
    }
 
    //返回mark()方法多次调用的平均间隔时间，单位是微秒
    public double getAverage() {
        return this.average;
    }
     
    //按照特定的时间单位来返回平均值,单位详见Time枚举
    public double getAverageIn(Time time) {
        return this.average == 0.0 ? this.average : this.average / time.getTime();
    }
     
    //返回特定时间度量内调用mark()的频率
    public double getAverageRatePer(Time time) {
        return this.average == 0.0 ? this.average : time.getTime() / this.average;
    }
}