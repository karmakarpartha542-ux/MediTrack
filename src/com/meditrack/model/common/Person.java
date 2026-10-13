package com.meditrack.model.common;

/**
 * =====================================================================
 * OOP CONCEPT: ENCAPSULATION + ABSTRACTION (Abstract Class)
 * =====================================================================
 * Person is an ABSTRACT BASE CLASS for all people in the hospital system.
 *
 * ENCAPSULATION:
 * - All fields are PRIVATE — they cannot be accessed directly from outside.
 * - Validated setters ensure invalid data (blank names, negative age) never
 *   enters the system. Internal state is fully protected.
 *
 * ABSTRACTION:
 * - getRole() is declared abstract. Subclasses MUST override it.
 * - Person cannot be instantiated directly — it is a blueprint only.
 *
 * SUBCLASSES (INHERITANCE):
 * - Patient extends Person
 * - Doctor  extends Person
 * =====================================================================
 */
public abstract class Person {

    // -------------------------
    // ENCAPSULATION: private fields
    // -------------------------
    private String id;
    private String name;
    private String phone;
    private String email;
    private int age;

    // -------------------------
    // Constructors
    // -------------------------

    /** Protected no-arg constructor for subclass use */
    protected Person() {}

    /**
     * Full constructor.
     *
     * @param id    unique identifier (e.g., "PAT-1001")
     * @param name  full name (cannot be blank)
     * @param phone contact phone number
     * @param email email address
     * @param age   age in years (must be positive)
     */
    public Person(String id, String name, String phone, String email, int age) {
        this.id = id;
        setName(name);   // use validated setter
        this.phone = phone;
        this.email = email;
        setAge(age);     // use validated setter
    }

    // -------------------------
    // ABSTRACTION: abstract method
    // -------------------------

    /**
     * Returns the role of this person (e.g., "Patient" or "Doctor").
     * Every subclass MUST implement this — it is abstract.
     *
     * @return role string
     */
    public abstract String getRole();

    // -------------------------
    // ENCAPSULATION: validated setters
    // -------------------------

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be blank.");
        }
        this.name = name.trim();
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAge(int age) {
        if (age <= 0 || age > 150) {
            throw new IllegalArgumentException("Age must be between 1 and 150.");
        }
        this.age = age;
    }

    public void setId(String id) {
        this.id = id;
    }

    // -------------------------
    // Getters (controlled read access)
    // -------------------------

    public String getId()    { return id; }
    public String getName()  { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public int    getAge()   { return age; }

    // -------------------------
    // JSON Serialization
    // -------------------------

    /**
     * Serializes Person fields to a partial JSON string.
     * Subclasses override this and append their own fields.
     *
     * @return JSON fragment string
     */
    public String toJson() {
        return "\"id\":\"" + escape(id) + "\"," +
               "\"name\":\"" + escape(name) + "\"," +
               "\"phone\":\"" + escape(phone) + "\"," +
               "\"email\":\"" + escape(email) + "\"," +
               "\"age\":" + age + "," +
               "\"role\":\"" + escape(getRole()) + "\"";
    }

    protected String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public String toString() {
        return getRole() + "[id=" + id + ", name=" + name + "]";
    }
}
