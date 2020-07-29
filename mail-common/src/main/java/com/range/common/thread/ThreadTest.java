package com.range.common.thread;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadTest {

    //当前系统只有一个线程池
    public static ExecutorService service = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {

//        //集成thread
        System.out.println("main...start...");
        Thread01 thread01 = new Thread01();
        thread01.start();
        System.out.println("main,...end...");
//
//        //实现runable接口
//        System.out.println("main...start...");
//        Runnable runnable = new Runable01();
//        new Thread(runnable).start();
//        System.out.println("main,...end...");

//        //实现Callable接口+FutureTask
//        System.out.println("main...start...");
//        FutureTask<Integer> futureTask = new FutureTask<>(new Callable01());
//        new Thread(futureTask).start();
//
//        //futureTask.get()可以等待整个线程完成获取返回结果
//        //是一个阻塞等待
//        Integer integer = futureTask.get();
//        System.out.println("futureTask.get()返回的结果"+integer);
//        System.out.println("main,...end...");

//        //线程池实现方式
//        System.out.println("main...start...");
//        //当前系统只有一个
//        service.execute(new Runable01());
//        System.out.println("main,...end...");

        /**
         * 方法完成的感知
         */
//        System.out.println("main...start...");
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前的线程：" + Thread.currentThread().getId());
//            int i = 10 / 0;
//            System.out.println("运行结果：" + i);
//            return i;
//        }).whenComplete((res, excption) -> {
//            //虽然能得到异常信息，但是没发修改返回数据
//            System.out.println("异步任务成功完成了...结果是："+res+"；异常是："+excption);
//        }).exceptionally(throwable -> {
//            //可以感知异常，同时返回默认值
//            return 10;
//        });
//        System.out.println("main,...end...");

        /**
         * 方法执行完成后的处理
         */
//        System.out.println("main...start...");
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前的线程：" + Thread.currentThread().getId());
//            int i = 10 / 0;
//            System.out.println("运行结果：" + i);
//            return i;
//        }).handle((res, thr) -> {
//            if (res != null) {
//                return res * 2;
//            }
//            if (thr != null) {
//                return 0;
//            }
//            return 0;
//        });
//        Integer integer = future.get();
//        System.out.println("main,...end..."+integer);




    }

    public static class Thread01 extends Thread{
        @Override
        public void run() {
            System.out.println("当前线程："+Thread.currentThread().getId());
            int i = 10 / 1;
//            try{
//                sleep(10000);
//            }catch (Exception e){
//
//            }
            System.out.println("运行结果：" + i);
        }
    }

    public static class Runable01 implements Runnable{

        @Override
        public void run() {
            System.out.println("当前线程："+Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("运行结果：" + i);
        }
    }

    public static class Callable01 implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            System.out.println("当前线程："+Thread.currentThread().getId());
            int i = 10 / 3;
            System.out.println("运行结果：" + i);
            return i;
        }
    }





}
