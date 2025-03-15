package cleverquiz.db;

import cleverquiz.model.Badge;
import cleverquiz.model.News;
import cleverquiz.model.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;

public class DBUtil {
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            Configuration configuration = new Configuration().configure();

            // Hibernate-Properties aus resources laden
            Properties properties = new Properties();
            InputStream input = DBUtil.class.getClassLoader().getResourceAsStream("hibernate-secret.properties");

            if (input == null) {
                throw new RuntimeException("hibernate-secret.properties nicht gefunden!");
            }

            properties.load(input);
            configuration.setProperties(properties);

            // Hier die User-Klasse explizit hinzufügen
            configuration.addAnnotatedClass(User.class);

            return configuration.buildSessionFactory();
        } catch (Exception ex) {
            throw new RuntimeException("Fehler beim Laden der Hibernate-Properties", ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static Session getSession() {
        return sessionFactory.openSession();
    }

    /**
     * Get the latest 15 news
     *
     * @return latest 15 news
     */
    public static List<News> getLatestNews() {
        Session session = DBUtil.getSession();
        Query<News> query = session.createQuery(
                "FROM News ORDER BY date DESC", News.class);
        query.setMaxResults(15);
        List<News> news = query.list();
        session.close();
        return news;
    }

    /**
     * Get 30 best users
     *
     * @return 30 best user by xp
     */
    public static List<User> getUserRanking() {
        Session session = DBUtil.getSession();
        Query<User> query = session.createQuery(
                "FROM User ORDER BY xp desc", User.class);
        query.setMaxResults(30);
        List<User> user = query.list();
        session.close();
        return user;
    }

    /**
     * Get badges owned by user
     *
     * @param userId owner
     * @return badges owned by user
     */
    public static List<Badge> getUserBadges(int userId) {
        List<Badge> badges = null;
        try (Session session = getSession()) {
            String sql = "SELECT b.* FROM Badge b " +
                    "JOIN User_Badge ub ON b.badgeId = ub.badgeId " +
                    "WHERE ub.userId = :userId";
            NativeQuery<Badge> query = session.createNativeQuery(sql, Badge.class);
            query.setParameter("userId", userId);
            badges = query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return badges;
    }




    public static void main(String[] args) {
        // Neue Session öffnen
        Session session = getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();

        // Neuen User speichern
        User newUser = new User();
        newUser.setName("TestUSer");
        newUser.setPassword("1234");
        newUser.setEmail("testMail");
        session.save(newUser);

        transaction.commit();

        // Benutzer aus der Datenbank abrufen
        User user = session.get(User.class, newUser.getUserId());
        System.out.println("Geladener Benutzer: " + user);

        session.close();
        getSessionFactory().close();
    }

    public static User login(String username, String password) {
        Session session = getSessionFactory().openSession();
        Transaction transaction = null;
        User user = null;

        try {
            transaction = session.beginTransaction();

            // HQL Query, um den User zu finden
            Query<User> query = session.createQuery("FROM User WHERE name = :username AND password = :password", User.class);
            query.setParameter("username", username);
            query.setParameter("password", password);  // Nur wenn das Passwort nicht gehasht gespeichert wird!

            user = query.uniqueResult(); // Falls kein Ergebnis, dann null zurückgeben

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
        return user;
    }
}
