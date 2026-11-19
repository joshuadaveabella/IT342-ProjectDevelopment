package edu.cit.abella.diladila.user;

/**
 * Account-level roles defined in the SRS (Section 2.2): Project Lead (LEAD) and Team Member (MEMBER).
 * A LEAD can do everything a MEMBER can do, plus the lead-only functions.
 */
public enum Role {
    LEAD,
    MEMBER
}
