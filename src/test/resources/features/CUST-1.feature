# Reference story. Every Jira story gets a file like this: src/test/resources/features/<JIRA-ID>.feature
@CUST-1
Feature: Register a customer
  As a customer service agent
  I want to register new customers
  So that they can place orders

  Background:
    Given I am authenticated with scope "customers.write customers.read"

  Scenario: Register a new customer
    When I register a customer named "Ada Lovelace" with email "Ada@Example.com"
    Then the response status is 201
    And the customer can be retrieved with email "ada@example.com" and status "ACTIVE"

  Scenario: Reject a duplicate email
    Given a customer with email "grace@example.com" exists
    When I register a customer named "Grace" with email "grace@example.com"
    Then the response status is 409
    And the error code is "CUSTOMER_EMAIL_ALREADY_EXISTS"

  Scenario Outline: Reject invalid input
    When I register a customer named "<name>" with email "<email>"
    Then the response status is 400
    And the error code is "VALIDATION_FAILED"

    Examples:
      | name | email        |
      |      | a@b.com      |
      | Bob  | not-an-email |

  Scenario: Reject a caller without the write scope
    Given I am authenticated with scope "customers.read"
    When I register a customer named "Eve" with email "eve@example.com"
    Then the response status is 403
