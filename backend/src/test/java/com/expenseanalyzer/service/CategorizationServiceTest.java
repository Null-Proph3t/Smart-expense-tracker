package com.expenseanalyzer.service;

import com.expenseanalyzer.model.ExpenseCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CategorizationServiceTest {

    private CategorizationService categorizationService;

    @BeforeEach
    void setUp() {
        categorizationService = new CategorizationService();
    }

    // -------------------------------------------------------------------------
    // 1. Whole-word regex boundary (\b) tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Whole-word matching: 'Business lunch' maps to FOOD, avoiding false match on 'bus'")
    void testBusinessLunchNotBus() {
        ExpenseCategory category = categorizationService.categorize("Business lunch with client");
        assertThat(category).isEqualTo(ExpenseCategory.FOOD);
    }

    @Test
    @DisplayName("Whole-word matching: 'Automobile servicing' does not falsely match 'auto' (TRANSPORTATION)")
    void testAutomobileNotAuto() {
        // 'auto' is a TRANSPORTATION keyword, but 'automobile' should not match as a whole word
        ExpenseCategory category = categorizationService.categorize("Automobile servicing and painting");
        assertThat(category).isEqualTo(ExpenseCategory.OTHERS);
    }

    @Test
    @DisplayName("Whole-word matching: 'Parental guidance seminar' does not falsely match 'rent' (BILLS_AND_UTILITIES)")
    void testParentalNotRent() {
        // 'rent' is in BILLS_AND_UTILITIES, but 'parental' should not match
        ExpenseCategory category = categorizationService.categorize("Parental guidance seminar pass");
        assertThat(category).isEqualTo(ExpenseCategory.OTHERS);
    }

    @Test
    @DisplayName("Whole-word matching: 'Billionaire biography' does not falsely match 'bill' (BILLS_AND_UTILITIES)")
    void testBillionaireNotBill() {
        // 'bill' is in BILLS_AND_UTILITIES, but 'billionaire' should not match
        ExpenseCategory category = categorizationService.categorize("Billionaire biography hardbound");
        // Note: 'books' is not in this string, so it should fall back to OTHERS
        assertThat(category).isEqualTo(ExpenseCategory.OTHERS);
    }

    @Test
    @DisplayName("Whole-word matching: 'Small boutique' does not falsely match 'mall' (SHOPPING)")
    void testSmallNotMall() {
        // 'mall' is in SHOPPING, but 'small' must not match
        ExpenseCategory category = categorizationService.categorize("Small boutique handmade craft");
        assertThat(category).isEqualTo(ExpenseCategory.OTHERS);
    }

    @Test
    @DisplayName("Whole-word matching: 'Cabin in woods' does not falsely match 'cab' (TRANSPORTATION)")
    void testCabinNotCab() {
        // 'cab' is in TRANSPORTATION, but 'cabin' must not match
        ExpenseCategory category = categorizationService.categorize("Weekend cabin stay resort");
        assertThat(category).isEqualTo(ExpenseCategory.OTHERS);
    }

    @Test
    @DisplayName("Whole-word matching: 'Collaborative study' does not falsely match 'lab' (HEALTHCARE)")
    void testCollaborativeNotLab() {
        // 'lab' is in HEALTHCARE, but 'collaborative' must not match
        ExpenseCategory category = categorizationService.categorize("Collaborative project workspace");
        assertThat(category).isEqualTo(ExpenseCategory.OTHERS);
    }

    // -------------------------------------------------------------------------
    // 2. Strict Category Precedence tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Category precedence: GROCERIES precedes SHOPPING ('Amazon Grocery' -> GROCERIES)")
    void testAmazonGroceryPrecedence() {
        ExpenseCategory category = categorizationService.categorize("Amazon Grocery order");
        assertThat(category).isEqualTo(ExpenseCategory.GROCERIES);
    }

    @Test
    @DisplayName("Category precedence: GROCERIES precedes FOOD ('Milk coffee' -> GROCERIES)")
    void testMilkCoffeePrecedence() {
        // 'milk' is GROCERIES, 'coffee' is FOOD. GROCERIES rule evaluated first.
        ExpenseCategory category = categorizationService.categorize("Fresh milk and coffee beans");
        assertThat(category).isEqualTo(ExpenseCategory.GROCERIES);
    }

    @Test
    @DisplayName("Category precedence: FOOD precedes TRANSPORTATION ('Lunch after uber' -> FOOD)")
    void testLunchUberPrecedence() {
        // 'lunch' is FOOD, 'uber' is TRANSPORTATION. FOOD evaluated first.
        ExpenseCategory category = categorizationService.categorize("Lunch after uber ride");
        assertThat(category).isEqualTo(ExpenseCategory.FOOD);
    }

    @Test
    @DisplayName("Category precedence: TRANSPORTATION precedes HEALTHCARE ('Cab to hospital' -> TRANSPORTATION)")
    void testCabHospitalPrecedence() {
        // 'cab' is TRANSPORTATION, 'hospital' is HEALTHCARE. TRANSPORTATION evaluated first.
        ExpenseCategory category = categorizationService.categorize("Cab ride to hospital");
        assertThat(category).isEqualTo(ExpenseCategory.TRANSPORTATION);
    }

    @Test
    @DisplayName("Category precedence: HEALTHCARE precedes BILLS_AND_UTILITIES ('Medical bill' -> HEALTHCARE)")
    void testMedicalBillPrecedence() {
        // 'medical' is HEALTHCARE, 'bill' is BILLS_AND_UTILITIES. HEALTHCARE evaluated first.
        ExpenseCategory category = categorizationService.categorize("Medical bill payment at counter");
        assertThat(category).isEqualTo(ExpenseCategory.HEALTHCARE);
    }

    // -------------------------------------------------------------------------
    // 3. Fallback categorization tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Fallback categorization: Unknown text, empty string, whitespace and null map to OTHERS")
    void testFallbackToOthers() {
        assertThat(categorizationService.categorize("Miscellaneous random item")).isEqualTo(ExpenseCategory.OTHERS);
        assertThat(categorizationService.categorize("Cash withdrawal from ATM")).isEqualTo(ExpenseCategory.OTHERS);
        assertThat(categorizationService.categorize("Settlement with friend")).isEqualTo(ExpenseCategory.OTHERS);
        assertThat(categorizationService.categorize("")).isEqualTo(ExpenseCategory.OTHERS);
        assertThat(categorizationService.categorize("   ")).isEqualTo(ExpenseCategory.OTHERS);
        assertThat(categorizationService.categorize(null)).isEqualTo(ExpenseCategory.OTHERS);
    }

    // -------------------------------------------------------------------------
    // 4. Case-insensitivity tests
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"SWIGGY", "swiggy", "SwIgGy", "sWiGgY", "SWIGGY FOOD ORDER"})
    @DisplayName("Case-insensitivity: variations of 'swiggy' map to FOOD")
    void testCaseInsensitivityFood(String input) {
        assertThat(categorizationService.categorize(input)).isEqualTo(ExpenseCategory.FOOD);
    }

    @ParameterizedTest
    @ValueSource(strings = {"UBER", "uber", "UbEr", "uBeR trip to airport"})
    @DisplayName("Case-insensitivity: variations of 'uber' map to TRANSPORTATION")
    void testCaseInsensitivityTransportation(String input) {
        assertThat(categorizationService.categorize(input)).isEqualTo(ExpenseCategory.TRANSPORTATION);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NETFLIX", "netflix", "NetFlix monthly subscription"})
    @DisplayName("Case-insensitivity: variations of 'netflix' map to ENTERTAINMENT")
    void testCaseInsensitivityEntertainment(String input) {
        assertThat(categorizationService.categorize(input)).isEqualTo(ExpenseCategory.ENTERTAINMENT);
    }

    // -------------------------------------------------------------------------
    // 5. Full keyword suite across all 9 categories
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("All specified category keywords match accurately across categories")
    void testAllCategoryKeywords() {
        // GROCERIES
        assertThat(categorizationService.categorize("Buy fresh milk")).isEqualTo(ExpenseCategory.GROCERIES);
        assertThat(categorizationService.categorize("Bigbasket morning order")).isEqualTo(ExpenseCategory.GROCERIES);
        assertThat(categorizationService.categorize("Zepto express delivery")).isEqualTo(ExpenseCategory.GROCERIES);
        assertThat(categorizationService.categorize("Blinkit veggies")).isEqualTo(ExpenseCategory.GROCERIES);
        assertThat(categorizationService.categorize("Supermarket essentials")).isEqualTo(ExpenseCategory.GROCERIES);
        assertThat(categorizationService.categorize("Fresh vegetables from mandi")).isEqualTo(ExpenseCategory.GROCERIES);
        assertThat(categorizationService.categorize("Seasonal fruits basket")).isEqualTo(ExpenseCategory.GROCERIES);

        // FOOD
        assertThat(categorizationService.categorize("Starbucks coffee")).isEqualTo(ExpenseCategory.FOOD);
        assertThat(categorizationService.categorize("McDonalds burger meal")).isEqualTo(ExpenseCategory.FOOD);
        assertThat(categorizationService.categorize("Dominos pizza party")).isEqualTo(ExpenseCategory.FOOD);
        assertThat(categorizationService.categorize("Sunday breakfast with family")).isEqualTo(ExpenseCategory.FOOD);
        assertThat(categorizationService.categorize("Italian restaurant dining")).isEqualTo(ExpenseCategory.FOOD);
        assertThat(categorizationService.categorize("Zomato dinner delivery")).isEqualTo(ExpenseCategory.FOOD);

        // TRANSPORTATION
        assertThat(categorizationService.categorize("Ola cab to office")).isEqualTo(ExpenseCategory.TRANSPORTATION);
        assertThat(categorizationService.categorize("Metro smart card recharge")).isEqualTo(ExpenseCategory.TRANSPORTATION);
        assertThat(categorizationService.categorize("Petrol pump fuel refilling")).isEqualTo(ExpenseCategory.TRANSPORTATION);
        assertThat(categorizationService.categorize("Diesel for generator")).isEqualTo(ExpenseCategory.TRANSPORTATION);
        assertThat(categorizationService.categorize("Train ticket reservation")).isEqualTo(ExpenseCategory.TRANSPORTATION);
        assertThat(categorizationService.categorize("Bus pass renewal")).isEqualTo(ExpenseCategory.TRANSPORTATION);
        assertThat(categorizationService.categorize("Auto fare via meter")).isEqualTo(ExpenseCategory.TRANSPORTATION);

        // HEALTHCARE
        assertThat(categorizationService.categorize("Doctor consultation fee")).isEqualTo(ExpenseCategory.HEALTHCARE);
        assertThat(categorizationService.categorize("Clinic visit for checkup")).isEqualTo(ExpenseCategory.HEALTHCARE);
        assertThat(categorizationService.categorize("Blood test at diagnostic lab")).isEqualTo(ExpenseCategory.HEALTHCARE);
        assertThat(categorizationService.categorize("Apollo pharmacy medicine purchase")).isEqualTo(ExpenseCategory.HEALTHCARE);
        assertThat(categorizationService.categorize("Hospital room charges")).isEqualTo(ExpenseCategory.HEALTHCARE);
        assertThat(categorizationService.categorize("General medical supplies")).isEqualTo(ExpenseCategory.HEALTHCARE);

        // EDUCATION
        assertThat(categorizationService.categorize("College semester reference books")).isEqualTo(ExpenseCategory.EDUCATION);
        assertThat(categorizationService.categorize("Coursera machine learning certification")).isEqualTo(ExpenseCategory.EDUCATION);
        assertThat(categorizationService.categorize("Udemy fullstack spring boot course")).isEqualTo(ExpenseCategory.EDUCATION);
        assertThat(categorizationService.categorize("Quarterly tuition fees")).isEqualTo(ExpenseCategory.EDUCATION);
        assertThat(categorizationService.categorize("Government exam registration fee")).isEqualTo(ExpenseCategory.EDUCATION);
        assertThat(categorizationService.categorize("Notebook and pen stationery")).isEqualTo(ExpenseCategory.EDUCATION);

        // BILLS_AND_UTILITIES
        assertThat(categorizationService.categorize("Monthly apartment rent payment")).isEqualTo(ExpenseCategory.BILLS_AND_UTILITIES);
        assertThat(categorizationService.categorize("High-speed broadband wifi bill")).isEqualTo(ExpenseCategory.BILLS_AND_UTILITIES);
        assertThat(categorizationService.categorize("State electricity board power bill")).isEqualTo(ExpenseCategory.BILLS_AND_UTILITIES);
        assertThat(categorizationService.categorize("Municipal water tax bill")).isEqualTo(ExpenseCategory.BILLS_AND_UTILITIES);
        assertThat(categorizationService.categorize("Airtel mobile prepaid recharge")).isEqualTo(ExpenseCategory.BILLS_AND_UTILITIES);
        assertThat(categorizationService.categorize("Fiber optic internet connection")).isEqualTo(ExpenseCategory.BILLS_AND_UTILITIES);

        // ENTERTAINMENT
        assertThat(categorizationService.categorize("Cinema multiplex movie tickets")).isEqualTo(ExpenseCategory.ENTERTAINMENT);
        assertThat(categorizationService.categorize("Spotify premium individual subscription")).isEqualTo(ExpenseCategory.ENTERTAINMENT);
        assertThat(categorizationService.categorize("Live musical concert tickets")).isEqualTo(ExpenseCategory.ENTERTAINMENT);
        assertThat(categorizationService.categorize("Steam pc gaming store purchase")).isEqualTo(ExpenseCategory.ENTERTAINMENT);

        // SHOPPING
        assertThat(categorizationService.categorize("Flipkart festive big sale")).isEqualTo(ExpenseCategory.SHOPPING);
        assertThat(categorizationService.categorize("Nike running shoes from store")).isEqualTo(ExpenseCategory.SHOPPING);
        assertThat(categorizationService.categorize("New winter clothing and jacket")).isEqualTo(ExpenseCategory.SHOPPING);
        assertThat(categorizationService.categorize("Summer casual clothes")).isEqualTo(ExpenseCategory.SHOPPING);
        assertThat(categorizationService.categorize("Consumer electronics gadget")).isEqualTo(ExpenseCategory.SHOPPING);
        assertThat(categorizationService.categorize("Phoenix mall shopping spree")).isEqualTo(ExpenseCategory.SHOPPING);
    }
}
