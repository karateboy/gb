<template>
    <div class="ibox flot-e-margins">
        <div class="ibox-content">
            <router-view></router-view>
        </div>
    </div>
</template>
<style scoped>

</style>
<script>
import { mapGetters } from "vuex";
export default {
  data() {
    return {};
  },
  computed: {
    ...mapGetters(["user"])
  }
};
</script>
