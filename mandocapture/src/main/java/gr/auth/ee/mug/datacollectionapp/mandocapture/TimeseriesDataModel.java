package gr.auth.ee.mug.datacollectionapp.mandocapture;

//import javax.xml.bind.annotation.XmlElement;
//import javax.xml.bind.annotation.XmlRootElement;

//@xmlRootElement
public class TimeseriesDataModel {
    private Preamble preamble;
    private Timeseries timeseries;

    //@xmlElement
    public Preamble getPreamble() {
        return preamble;
    }
    public void setPreamble (Preamble preamble) {
        this.preamble = preamble;
    }

    //@xmlElement
    public Timeseries getTimeseries() {
        return timeseries;
    }
    public void setTimeseries(Timeseries timeseries) {
        this.timeseries = timeseries;
    }

    public class Timeseries {
        private double[] values;
        private double[] probabilities;

        //@xmlElement
        public double[] getValues() {
            return values;
        }
        public void setValues(double[] values) {
            this.values = values;
        }
        //@xmlElement
        public double[] getProbabilities() {
            return probabilities;
        }
        public void setProbabilities(double[] probabilities) {
            this.probabilities = probabilities;
        }
    }

    public class Preamble {
        //@xmlElement
        private String type;
        private Double samplingRate;
        private Integer samples;
        private String[] sensors;
        private String startTimestamp;
        private String endTimestamp;

        //@xmlElement
        public String getType() {
            return type;
        }
        public void setType(String type) {
            this.type = type;
        }
        //@xmlElement
        public Double getSamplingRate() {
            return samplingRate;
        }
        public void setSamplingRate(Double samplingRate) {
            this.samplingRate = samplingRate;
        }
        //@xmlElement
        public Integer getSamples() {
            return samples;
        }
        public void setSamples(Integer samples) {
            this.samples = samples;
        }
        //@xmlElement
        public String[] getSensors() {
            return sensors;
        }
        public void setSensors(String[] sensors) {
            this.sensors = sensors;
        }
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
    }
}
