package models.mutable;

import javax.persistence.*;
import java.util.Collection;

/**
 * User: dweinberg
 * Date: 12/20/13
 * Time: 11:12 PM
 */
@Entity
public class Company {
    private long id;
    private long version;
    private String name;

    @Column(name = "ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Version
    @Column(name = "version")
    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    @Basic
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private Collection<Computer> computers;

    @OneToMany(mappedBy = "company")
    public Collection<Computer> getComputers() {
        return computers;
    }

    public void setComputers(Collection<Computer> computers) {
        this.computers = computers;
    }
}
