package narayana.performance.beans;

import narayana.performance.util.Result;

public interface HelloWorld {
//    String doWork(Result opts);
    Result doWork(Result opts, boolean iiop, boolean ejb2, String namingProvider);
}
