package org.multibit.mbm.api.request;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * <p>Request to provide the following to Resources:</p>
 * <ul>
 * <li>Provision of client state to delete the details of an existing User by an administrator</li>
 * </ul>
 * <p>When an administrator deletes a User there are special .</p>
 *
 * @since 0.0.1
 *         
 */
public class AdminDeleteEntityRequest {

  /**
   * The ID of the entity
   */
  @JsonProperty
  private String id = null;

  /**
   * The reason for deleting (archiving) the entity
   */
  @JsonProperty
  private String reason = null;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
