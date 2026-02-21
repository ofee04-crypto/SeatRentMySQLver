package com.example.backend.utils;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class HibernateUtil {

    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            // 1. 建立 ServiceRegistry（載入 hibernate.cfg.xml 設定）
            StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .configure("hibernate.cfg.xml") // 預設會找 src/main/resources 下的設定檔
                    .build();

            // 2. 建立 Metadata（解析 Entity 註解，如你的 discountBean）
            Metadata metadata = new MetadataSources(serviceRegistry)
                    .getMetadataBuilder()
                    .build();

            // 3. 產出 SessionFactory
            return metadata.getSessionFactoryBuilder().build();
            
        } catch (Exception e) {
            System.err.println("SessionFactory 建立失敗：" + e);
            throw new ExceptionInInitializerError(e);
        }
    }

    // 取得 SessionFactory 的靜態方法
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    // 關閉 SessionFactory（通常在應用程式停止時呼叫）
    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
    }
}