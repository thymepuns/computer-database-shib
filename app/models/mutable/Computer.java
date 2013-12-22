package models.mutable;

import javax.persistence.*;
import java.util.Date;

/**
 * User: dweinberg
 * Date: 12/20/13
 * Time: 11:14 PM
 */
@Entity
public class Computer {
    private long id;
    private long version;
    private String name;
    private Date introduced;
    private Date discontinued;

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

    @Basic
    public Date getIntroduced() {
        return introduced;
    }

    public void setIntroduced(Date introduced) {
        this.introduced = introduced;
    }

    @Basic
    public Date getDiscontinued() {
        return discontinued;
    }

    public void setDiscontinued(Date discontinued) {
        this.discontinued = discontinued;
    }

    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", referencedColumnName = "ID", nullable = true)
    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }
}
