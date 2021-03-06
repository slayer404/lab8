package src.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import src.database.DBManager;
import src.database.User;
import src.elements.*;
import src.server.Server;

import javax.xml.bind.ValidationException;
import java.io.IOException;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Class which manages collection.
 */

public class CollectionManager {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private TreeSet<Product> products;
    private HashMap<String, java.awt.Color> usersColors;

    private DBManager dbManager;
    private LocalDateTime creationDate;
    private DefaultQueue history;
    private Scanner scanner;

    private boolean exit = false;
    private boolean hasChanges = false;

    private ReadWriteLock lock;

    private final int MAX_COLOR_CODE = 255;

    private static int freeId;

    /**
     * Constructor
     */

    public CollectionManager(DBManager dbManager) {

        products = new TreeSet<>();
        usersColors = new HashMap<>();
        creationDate = LocalDateTime.now();
        history = new DefaultQueue(11);
        scanner = new Scanner(System.in);
        this.dbManager = dbManager;
        products.addAll(dbManager.readAllProducts());
        defineUserColors();
        lock = new ReentrantReadWriteLock();
    }

    /**
     * Modifies history of used src.commands.
     * @param command - the next command
     */

    private void modifyHistory(String command)
    {
        history.insert(command);
    }

    /**
     * Finds a max price of src.elements in collection
     * return the maximum value
     */

    private boolean isIdBusy(int id) {
        if (!products.isEmpty()) {
            return products.stream().anyMatch(p->p.getId() == id);
        } else {
            return false;
        }
    }

    private Long findMax() {
        Product max = products.stream()
                .max(Comparator.comparing(Product::getPrice))
                .get();

        return max.getPrice();
    }

    /**
     * Finds a min price of src.elements in collection
     * @return the minimal value
     */

    private long findMin() {
        PriceComparator comparator = new PriceComparator();
        Product min = products.stream()
                .min(Comparator.comparing(Product::getPrice))
                .get();

        return min.getPrice();
    }

    /**
     * Adds a new element to collection
     */

    private void defineUserColors() {
        for (Product product : products) {
            if (usersColors.get(product.getHost()) == null) {
                usersColors.put(product.getHost(), new java.awt.Color((int) (Math.random() * MAX_COLOR_CODE), (int) (Math.random() * MAX_COLOR_CODE), (int) (Math.random() * MAX_COLOR_CODE)));
            }
            product.setColor(usersColors.get(product.getHost()));
        }
    }

    private void defineUserColors(Product product) {
        if (usersColors.get(product.getHost()) == null) {
            usersColors.put(product.getHost(), new java.awt.Color((int) (Math.random() * MAX_COLOR_CODE), (int) (Math.random() * MAX_COLOR_CODE), (int) (Math.random() * MAX_COLOR_CODE)));
        }
        product.setColor(usersColors.get(product.getHost()));
    }

    public String add(Object object) {
        lock.writeLock().lock();
        Product product = (Product) object;
        modifyHistory("add");
        int id = dbManager.createProduct(product);
        if (id  != -1) {
            product.setId(id);
            products.add(product);
            defineUserColors(product);
            lock.writeLock().unlock();
            hasChanges = true;
            return "Product was successfully added to the collection.\n";
        } else {
            lock.writeLock().unlock();
            return "There are some problems with adding a product to collection.\n";
        }
    }

    /**
     * Adds an element to collection if it is max
     * @throws ValidationException
     */

    public String addIfMax(Object object) {
        lock.writeLock().lock();
        Product product = (Product) object;
        modifyHistory("add_if_max");
        if (product.getPrice() > findMax()) {
            int id = dbManager.createProduct(product);
            if (id != -1) {
                products.add(product);
                defineUserColors(product);
                lock.writeLock().unlock();
                hasChanges = true;
                return "Product was successfully added to the collection.\n";
            } else {
                lock.writeLock().unlock();
                return "There are some problems with adding a product to collection.\n";
            }
        } else {
            lock.writeLock().unlock();
            return "You are trying to add the product which isn't a max!\n";
        }
    }

    /**
     * Adds a new element to collection if it is min
     */

    public String addIfMin(Object object) {
        lock.writeLock().lock();
        Product product = (Product) object;
        modifyHistory("add_if_min");
        if (product.getPrice() < findMin()) {
            int id = dbManager.createProduct(product);
            if (id != -1) {
                products.add(product);
                defineUserColors(product);
                hasChanges = true;
                lock.writeLock().unlock();
                return "Product was successfully added to the collection.\n";
            } else {
                lock.writeLock().unlock();
                return "There are some problems with adding a product to collection.\n";
            }
        }
        lock.writeLock().unlock();
        return "You are trying to add the product which isn't a min!\n";
    }

    /**
     * Clears collection
     */

    public String clear(User user) {
        lock.writeLock().lock();

        ArrayList<Product> productsToRemove = new ArrayList<>();
        for (Product p : products) {
            if (p.getHost().equals(user.getLogin())) {
                productsToRemove.add(p);

            }
        }

        for (Product p : productsToRemove) {
            if (products.contains(p)) {
                products.remove(p);
                dbManager.deleteProduct(p.getId());
            }
        }

        hasChanges = true;
        modifyHistory("clear");
        lock.writeLock().unlock();
        return "The collection was cleared.\n";
    }

    /**
     * Just for saving in history command execute script
     */

    public String executeScript() {
        lock.writeLock().lock();
        modifyHistory("execute_script");
        lock.writeLock().unlock();
        return "A new script was started to execute\n";
    }

    /**
     * Filters collection by unit of measure
     */

    public String filterByUnitOfMeasure(Object object) {
        lock.readLock().lock();
        String unitOfMeasure = (String) object;
        String result = "The result of filtering by unit of measure:\n";

        List<Product> res = products.stream()
                .filter(Product -> unitOfMeasure.equals(Product.getUnitOfMeasure().getUnit()))
                .collect(Collectors.toList());

        for (Product p : res) {
            result += p.getName() + "\n";
        }

        lock.readLock().lock();

        modifyHistory("filter_by_unit_of_measure");

        return result;
    }

    /**
     * Shows the list of available src.commands
     */

    public String help() {
        logger.info("help");
        modifyHistory("help");
        return "//// HELP //// " +
                "\ninfo : вывести в стандартный поток вывода информацию о коллекции (тип, дата инициализации, количество элементов и т.д.)" +
                "\nshow : вывести в стандартный поток вывода все элементы коллекции в строковом представлении" +
                "\nadd {element} : добавить новый элемент в коллекцию" +
                "\nupdate id {element} : обновить значение элемента коллекции, id которого равен заданному" +
                "\nremove_by_id id : удалить элемент из коллекции по его id" +
                "\nclear : очистить коллекцию" +
                "\nsave : сохранить коллекцию в файл" +
                "\nexecute_script file_name : считать и исполнить скрипт из указанного файла. В скрипте содержатся команды в таком же виде, в котором их вводит пользователь в интерактивном режиме" +
                "\nexit : завершить программу (без сохранения в файл)" +
                "\nadd_if_max {element} : добавить новый элемент в коллекцию, если его значение превышает значение наибольшего элемента этой коллекции" +
                "\nadd_if_min {element} : добавить новый элемент в коллекцию, если его значение меньше, чем у наименьшего элемента этой коллекции" +
                "\nhistory : вывести последние 11 команд (без их аргументов)" +
                "\nfilter_by_unit_of_measure unitOfMeasure : вывести элементы, значение поля unitOfMeasure которых равно заданному" +
                "\nprint_unique_part_number partNumber : вывести уникальные значения поля partNumber" +
                "\nprint_field_descending_owner owner : вывести значения поля owner в порядке убывания\n";
    }

    /**
     * Shows history of last used commands
     */

    public String history() {
        lock.readLock().lock();
        String result = "The history of your last used commands:\n";
        for(int i = 0; i < history.getSize(); i++) {
            result += history.getElement(i) + "\n";
        }
        lock.readLock().unlock();
        modifyHistory("history");
        return result;
    }

    /**
     * Shows info about collection
     */

      public String info() {
        lock.readLock().lock();
        modifyHistory("info");
        try {
            Field treeSetField = CollectionManager.class.getDeclaredField("products");
            String treeSetType = treeSetField.getGenericType().getTypeName();
            if (!products.isEmpty()) {
                lock.readLock().unlock();
                return "Тип: " + products.getClass().getName() + "<" + treeSetType + ">" + "\nДата Создания" + creationDate + "\nРазмер: " + products.size() + "\n";
            } else {
                lock.readLock().unlock();
                return "Type can not be defined because collection is empty! " + "\nCreation Date" + creationDate + "\nSize: " + products.size() + "\n";
            }
        } catch (NoSuchFieldException ex) {
            lock.readLock().unlock();
            return "Problem with general class. Can not find type of class!\n";
        }
    }

    /**
     * Prints owners in decreasing order
     */

    public String printFieldDescendingOwner() {
        lock.readLock().lock();
        ArrayList<Person> ownersList = new ArrayList<>();
        String result = "The owners:\n";

        for (Product product: products) {
            ownersList.add(product.getOwner());
        }

        List<Person> answ = ownersList.stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        for (Person p : ownersList) {
            result += p.getName() + "\n";
        }
        lock.readLock().unlock();
        modifyHistory("print_field_descending_owner");
        return result;
    }

    /**
     * Removes an element by id
     */

    public String removeById(User user, Object object) {
        lock.writeLock().lock();
        Integer id = (Integer) object;

        modifyHistory("remove_by_id");

        for (Product p : products) {
            if (p.getId() == id) {
                if (p.getHost().equals(user.getLogin())) {
                    products.remove(p);
                    dbManager.deleteProduct(id);
                    hasChanges = true;
                    lock.writeLock().unlock();
                    return "Element was successfully removed.\n";
                }
                else {
                    lock.writeLock().unlock();
                    return "You don't have a permission to change this element!\n";
                }
            }
        }
        lock.writeLock().unlock();
        return "The element with this id wasn't found.\n";
    }

    /**
     * Shows collection in string presentation
     */

    public ArrayList<Product> show() {
        lock.readLock().lock();

        if (!products.isEmpty()) {
            ArrayList<Product> result = new ArrayList<>(products);
            lock.readLock().unlock();
            return result;
        }
        lock.readLock().unlock();
        return null;
    }

    /**
     * Prints unique numbers of parts
     */

    public String printUniquePartNumber() {
        lock.readLock().lock();
        ArrayList<String> partNumbers = new ArrayList<>();
        String result = new String();
        for(Product product: products) {
            partNumbers.add(product.getPartNumber());
        }
        HashSet <String> uniqueNumbers = new HashSet<>(partNumbers);

        modifyHistory("print_unique_part_number");

        for (String s : uniqueNumbers) {
            result += s + "\n";
        }
        lock.readLock().unlock();
        return result;
    }

    /**
     * Updates id of element
     */

    public String updateId(User user, Object object) {
        lock.writeLock().lock();
        Product product = (Product) object;

        modifyHistory("update_id");

        for (Product p : products) {
            if (p.getId() == product.getId()) {
                if (p.getHost().equals(user.getLogin())) {
                    products.remove(p);
                    products.add(product);
                    dbManager.updateProduct(product);
                    hasChanges = true;
                    lock.writeLock().unlock();
                    return "The element's id was successfully updated!\n";
                }
                lock.writeLock().unlock();
                return "You don't have a permission to change this element!\n";
            }
        }
        lock.writeLock().unlock();
        return "This id is busy.\n";

    }

    public boolean isChanged() {
        return hasChanges;
    }

    public void handleChanges() {
        hasChanges = false;
    }
}