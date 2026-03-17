package com.rsargsyan.sprite.main_ctx.core.domain.aggregate;

import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.EmbeddedJobSpec;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.ThumbnailConfig;
import com.rsargsyan.sprite.main_ctx.core.exception.InvalidThumbnailConfigException;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import org.hibernate.annotations.Type;

import java.util.HashSet;
import java.util.List;

@Entity
@Getter
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "name"}))
public class JobSpec extends AccountScopedAggregateRoot {
  private static final int MAX_NAME_LENGTH = 127;
  private static final int MAX_DESCRIPTION_LENGTH = 255;
  private static final int MAX_CONFIGS = 10;

  private String name;
  private String description;

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb")
  private List<ThumbnailConfig> configs;

  @SuppressWarnings("unused")
  JobSpec() {}

  public JobSpec(Account account, String name, String description, List<ThumbnailConfig> configs) {
    super(account);
    if (name == null || name.isBlank()) throw new InvalidThumbnailConfigException("Name is required");
    if (name.length() > MAX_NAME_LENGTH) throw new InvalidThumbnailConfigException("Name must not exceed %d characters".formatted(MAX_NAME_LENGTH));
    if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) throw new InvalidThumbnailConfigException("Description must not exceed %d characters".formatted(MAX_DESCRIPTION_LENGTH));
    if (configs == null || configs.isEmpty()) throw new InvalidThumbnailConfigException("At least one config is required");
    if (configs.size() > MAX_CONFIGS) throw new InvalidThumbnailConfigException("Job spec cannot have more than %d configs".formatted(MAX_CONFIGS));
    if (configs.size() != new HashSet<>(configs).size()) throw new InvalidThumbnailConfigException("Job spec cannot contain duplicate configs");
    var folderNames = configs.stream().map(ThumbnailConfig::folderName).toList();
    if (folderNames.size() != new HashSet<>(folderNames).size()) throw new InvalidThumbnailConfigException("Job spec cannot contain configs with duplicate folder names");
    this.name = name.trim();
    this.description = description;
    this.configs = List.copyOf(configs);
  }

  public EmbeddedJobSpec toEmbedded() {
    return new EmbeddedJobSpec(configs);
  }
}
