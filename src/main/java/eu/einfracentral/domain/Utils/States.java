package eu.einfracentral.domain.Utils;

public class States {

    public enum ProviderStates {
        APPROVED("approved"),
        PENDING_1("pending initial approval"),
        PENDING_2("pending service template approval");

        private final String type;

        ProviderStates(final String type) {
            this.type = type;
        }

        public String getKey() {
            return type;
        }
        }

    public enum ServiceStates {
        APPROVED("approved");

        private final String type;

        ServiceStates(final String type) {
            this.type = type;
        }

        public String getKey() {
            return type;
        }
    }
}
