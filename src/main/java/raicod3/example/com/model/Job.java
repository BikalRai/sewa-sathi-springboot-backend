package raicod3.example.com.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import raicod3.example.com.dto.job.JobRequestDto;
import raicod3.example.com.enums.JobDifficulty;
import raicod3.example.com.enums.JobStatus;
import raicod3.example.com.enums.Urgency;
import raicod3.example.com.utilities.AddressUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Job extends AbstractBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerProfile customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private JobCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Urgency urgency;

    @Column(nullable = false, columnDefinition = "TEXT") // TEXT is better for descriptions
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bid> bids = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "job_images", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "image_url")
    private List<String> images = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "job_completion_images", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "image_url")
    private List<String> completionImages = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String completionNotes;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(length = 255)
    private String maskedAddress;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private String contactNumber; // Added to capture the frontend input

    @Enumerated(EnumType.STRING)
    private JobDifficulty difficulty;

    private LocalDateTime expiresAt;
    private LocalDateTime completedAt;


    protected void onJobCreate() {
        // AbstractBaseEntity usually handles created_at/updated_at
        this.expiresAt = LocalDateTime.now().plusHours(1);

        // Start in an analyzing state so the queue can process it before it goes live
        if (this.status == null) {
            this.status = JobStatus.ANALYZING;
        }
    }

    // Notice we pass the imageUrls separately after the service layer uploads them
    public Job(JobRequestDto dto, CustomerProfile customer, JobCategory category) {
        this.customer = customer;
        this.category = category;
        this.urgency = Urgency.valueOf(dto.getUrgency().toUpperCase()); // Ensure enum mapping is safe
        this.description = dto.getDescription();
        this.address = dto.getAddress();
        this.maskedAddress = AddressUtils.generateMaskedAddress(dto.getAddress());
        this.latitude = dto.getLatitude(); // Adjusted to match standard DTO naming
        this.longitude = dto.getLongitude(); // Adjusted to match standard DTO naming
        this.contactNumber = dto.getPhoneNumber();
        this.images = dto.getImages(); // Grab the URLs directly from the DTO
    }
}
