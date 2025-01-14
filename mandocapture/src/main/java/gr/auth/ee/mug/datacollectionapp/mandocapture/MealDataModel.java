package gr.auth.ee.mug.datacollectionapp.mandocapture;

//import javax.xml.bind.annotation.XmlElement;
//import javax.xml.bind.annotation.XmlRootElement;

//@xmlRootElement
public class MealDataModel {
    private String idMeal;
    private Double averageBiteFrequency;
    private Double averageChewingRate;
    private Double averageFoodIntakeRate;
    private Double averageBiteSize;
    private Double chewingRateAcceleration;
    private Boolean hasChewingSensor;
    private Boolean hasMandometerSensor;
    private Double initialBiteSize;
    private Double initialChewingRate;
    private Boolean isConfirmedSnack;
    private Boolean isRegisteredMeal;
    private Double mealCurveAlpha;
    private Double mealCurveBeta;
    private Double mealDetectionProbability;
    private Double mealDuration;
    private String mealStartTime;
    private String mealEndTime;
    private String mealType;
    private Double numberOfFoodAdditions;
    private Double satietyFoodIntakeRatio;
    private Double totalFoodIntake;
    private Double biteSizeStandardDeviation;
    private Double biteSizeRate;
    private Double biteSizeDeceleration;
    private Double weightOfLeftOvers;
    private Integer satietyBeforeMeal;
    private Integer satietyAfterMeal;
    private Boolean likedFood;
    private TimeseriesDataModel mandometerRawData;
    private TimeseriesDataModel mandometerProcessedData;
    private FoodAddition[] foodAdditions;
    private FoodStructure foodStructure;
    private FoodComponents foodComponents;
    private DrinkComponents drinkComponents;

    //@xmlRootElement
    public class FoodComponents {
        private Integer noComponents;
        private String[] type;
        private String[] preparation;
        private String[] size;

        //@xmlElement
        public Integer getNoComponents() {
            return noComponents;
        }
        public void setNoComponents(Integer noComponents) {
            this.noComponents = noComponents;
        }
        //@xmlElement
        public String[] getType() {
            return type;
        }
        public void setType(String[] type) {
            this.type = type;
        }
        //@xmlElement
        public String[] getPreparation() {
            return preparation;
        }
        public void setPreparation(String[] preparation) {
            this.preparation = preparation;
        }
        //@xmlElement
        public String[] getSize() {
            return size;
        }
        public void setSize(String[] size) {
            this.size = size;
        }
    }

    //@xmlRootElement
    public class DrinkComponents {
        private Integer noComponents;
        private String[] type;
        private String[] extra;
        private String[] size;

        //@xmlElement
        public Integer getNoComponents() {
            return noComponents;
        }
        public void setNoComponents(Integer noComponents) {
            this.noComponents = noComponents;
        }
        //@xmlElement
        public String[] getType() {
            return type;
        }
        public void setType(String[] type) {
            this.type = type;
        }
        //@xmlElement
        public String[] getExtra() {
            return extra;
        }
        public void setExtra(String[] extra) {
            this.extra = extra;
        }
        //@xmlElement
        public String[] getSize() {
            return size;
        }
        public void setSize(String[] size) {
            this.size = size;
        }
    }

    //@xmlRootElement
    public class FoodAddition {
        private String startTimestamp;
        private String endTimestamp;
        private Double weight;

        //@xmlElement
        public String getStartTimestamp() {
            return startTimestamp;
        }
        public void setStartTimestamp(String startTimestamp) {
            this.startTimestamp = startTimestamp;
        }
        //@xmlElement
        public String getEndTimestamp() {
            return endTimestamp;
        }
        public void setEndTimestamp(String endTimestamp) {
            this.endTimestamp = endTimestamp;
        }
        //@xmlElement
        public double getWeight() {
            return weight;
        }
        public void setWeight(double weight) {
            this.weight = weight;
        }
    }

    public class FoodStructure {
        private String type;
        private Boolean isCrispy;
        private Boolean isChewy;
        private Boolean isWet;

        //@xmlElement
        public String getType() {
            return type;
        }
        public void setType(String type) {
            this.type = type;
        }
        //@xmlElement
        public Boolean getIsCrispy() {
            return isCrispy;
        }
        public void setIsCrispy(Boolean isCrispy) {
            this.isCrispy = isCrispy;
        }
        //@xmlElement
        public Boolean getIsChewy() {
            return isChewy;
        }
        public void setIsChewy(Boolean isChewy) {
            this.isChewy = isChewy;
        }
        //@xmlElement
        public Boolean getIsWet() {
            return isWet;
        }
        public void setIsWet(Boolean isWet) {
            this.isWet = isWet;
        }
    }

    //@xmlElement
    public String getIdMeal() {
        return idMeal;
    }
    public void setIdMeal(String mealID) {
        this.idMeal = mealID;
    }
    //@xmlElement
    public Double getAverageBiteFrequency() {
        return averageBiteFrequency;
    }
    public void setAverageBiteFrequency(Double averageBiteFrequency) {
        this.averageBiteFrequency = averageBiteFrequency;
    }
    //@xmlElement
    public Double getAverageChewingRate() {
        return averageChewingRate;
    }
    public void setAverageChewingRate(Double averageChewingRate) {
        this.averageChewingRate = averageChewingRate;
    }
    //@xmlElement
    public Double getAverageFoodIntakeRate() {
        return averageFoodIntakeRate;
    }
    public void setAverageFoodIntakeRate(Double averageFoodIntake) {
        this.averageFoodIntakeRate = averageFoodIntake;
    }
    //@xmlElement
    public Double getAverageBiteSize() {
        return averageBiteSize;
    }
    public void setAverageBiteSize(Double biteSize) {
        this.averageBiteSize = biteSize;
    }
    //@xmlElement
    public Double getChewingRateAcceleration() {
        return chewingRateAcceleration;
    }
    public void setChewingRateAcceleration(Double chewingRateAcceleration) {
        this.chewingRateAcceleration = chewingRateAcceleration;
    }
    //@xmlElement
    public Boolean getHasChewingSensor() {
        return hasChewingSensor;
    }
    public void setHasChewingSensor(Boolean hasChewingSensor) {
        this.hasChewingSensor = hasChewingSensor;
    }
    //@xmlElement
    public Boolean getHasMandometerSensor() {
        return hasMandometerSensor;
    }
    public void setHasMandometerSensor(Boolean hasMandometerSensor) {
        this.hasMandometerSensor = hasMandometerSensor;
    }
    //@xmlElement
    public Double getInitialBiteSize() {
        return initialBiteSize;
    }
    public void setInitialBiteSize(Double initialBiteSize) {
        this.initialBiteSize = initialBiteSize;
    }
    //@xmlElement
    public Double getInitialChewingRate() {
        return initialChewingRate;
    }
    public void setInitialChewingRate(Double initialChewingRate) {
        this.initialChewingRate = initialChewingRate;
    }
    //@xmlElement
    public Boolean getIsConfirmedSnack() {
        return isConfirmedSnack;
    }
    public void setIsConfirmedSnack(Boolean isConfirmedSnack) {
        this.isConfirmedSnack = isConfirmedSnack;
    }
    //@xmlElement
    public Boolean getIsRegisteredMeal() {
        return isRegisteredMeal;
    }
    public void setIsRegisteredMeal(Boolean isRegisteredMeal) {
        this.isRegisteredMeal = isRegisteredMeal;
    }
    //@xmlElement
    public Double getMealCurveAlpha() {
        return mealCurveAlpha;
    }
    public void setMealCurveAlpha(Double mealCurveAlpha) {
        this.mealCurveAlpha = mealCurveAlpha;
    }
    //@xmlElement
    public Double getMealCurveBeta() {
        return mealCurveBeta;
    }
    public void setMealCurveBeta(Double mealCurveBeta) {
        this.mealCurveBeta = mealCurveBeta;
    }
    //@xmlElement
    public Double getMealDetectionProbability() {
        return mealDetectionProbability;
    }
    public void setMealDetectionProbability(Double mealDetectionProbability) {
        this.mealDetectionProbability = mealDetectionProbability;
    }
    //@xmlElement
    public Double getMealDuration() {
        return mealDuration;
    }
    public void setMealDuration(Double mealDuration) {
        this.mealDuration = mealDuration;
    }
    //@xmlElement
    public String getMealStartTime() {
        return mealStartTime;
    }
    public void setMealStartTime(String mealStartTime) {
        this.mealStartTime = mealStartTime;
    }
    //@xmlElement
    public String getMealEndTime() {
        return mealEndTime;
    }
    public void setMealEndTime(String mealEndTime) {
        this.mealEndTime = mealEndTime;
    }
    //@xmlElement
    public String getMealType() {
        return mealType;
    }
    public void setMealType(String mealType) {
        this.mealType = mealType;
    }
    //@xmlElement
    public Double getNumberOfFoodAdditions() {
        return numberOfFoodAdditions;
    }
    public void setNumberOfFoodAdditions(Double numberOfFoodAdditions) {
        this.numberOfFoodAdditions = numberOfFoodAdditions;
    }
    //@xmlElement
    public Double getSatietyFoodIntakeRatio() {
        return satietyFoodIntakeRatio;
    }
    public void setSatietyFoodIntakeRatio(Double satietyFoodIntakeRatio) {
        this.satietyFoodIntakeRatio = satietyFoodIntakeRatio;
    }
    //@xmlElement
    public Double getTotalFoodIntake() {
        return totalFoodIntake;
    }
    public void setTotalFoodIntake(Double totalFoodIntake) {
        this.totalFoodIntake = totalFoodIntake;
    }
    //@xmlElement
    public Double getBiteSizeStandardDeviation() {
        return biteSizeStandardDeviation;
    }
    public void setBiteSizeStandardDeviation(Double varianceOfBiteSize) {
        this.biteSizeStandardDeviation = varianceOfBiteSize;
    }
    //@xmlElement
    public Double getWeightOfLeftOvers() {
        return weightOfLeftOvers;
    }
    public void setWeightOfLeftOvers(Double weightOfLeftOvers) {
        this.weightOfLeftOvers = weightOfLeftOvers;
    }
    //@xmlElement
    public TimeseriesDataModel getMandometerRawData() {
        return mandometerRawData;
    }
    public void setMandometerRawData(TimeseriesDataModel mandometerRawData) {
        this.mandometerRawData = mandometerRawData;
    }
    //@xmlElement
    public TimeseriesDataModel getHasMandometerProcessedData() {
        return mandometerProcessedData;
    }
    public void setMandometerProcessedData(TimeseriesDataModel mandometerProcessedData) {
        this.mandometerProcessedData = mandometerProcessedData;
    }
    //@xmlElement
    public FoodAddition[] getFoodAdditions() {
        return foodAdditions;
    }
    public void setFoodAdditions(FoodAddition[] foodAdditions) {
        this.foodAdditions = foodAdditions;
    }
    //@xmlElement
    public FoodStructure getFoodStructure() {
        return foodStructure;
    }
    public void setFoodStructure(FoodStructure hasFoodStructure) {
        this.foodStructure = hasFoodStructure;
    }
    //@xmlElement
    public Double getBiteSizeRate() {
        return biteSizeRate;
    }
    public void setBiteSizeRate(Double biteSizeRate) {
        this.biteSizeRate = biteSizeRate;
    }
    //@xmlElement
    public Double getBiteSizeDeceleration() {
        return biteSizeDeceleration;
    }
    public void setBiteSizeDeceleration(Double biteSizeDeceleration) {
        this.biteSizeDeceleration = biteSizeDeceleration;
    }
    //@xmlElement
    public Integer getSatietyBeforeMeal() {
        return satietyBeforeMeal;
    }
    public void setSatietyBeforeMeal(Integer satietyBeforeMeal) {
        this.satietyBeforeMeal = satietyBeforeMeal;
    }
    //@xmlElement
    public Integer getSatietyAfterMeal() {
        return satietyAfterMeal;
    }
    public void setSatietyAfterMeal(Integer satietyAfterMeal) {
        this.satietyAfterMeal = satietyAfterMeal;
    }
    //@xmlElement
    public Boolean getLikedFood() {
        return likedFood;
    }
    public void setLikedFood(Boolean likedFood) {
        this.likedFood = likedFood;
    }
    //@xmlElement
    public FoodComponents getFoodComponents() {
        return foodComponents;
    }
    public void setFoodComponents(FoodComponents foodComponents) {
        this.foodComponents = foodComponents;
    }
    //@xmlElement
    public DrinkComponents getDrinkComponents() {
        return drinkComponents;
    }
    public void setDrinkComponents(DrinkComponents drinkComponents) {
        this.drinkComponents = drinkComponents;
    }

}
