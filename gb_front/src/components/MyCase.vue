<template>
    <div>
        <build-case2-list url="/MyCase" :param="{ sortBy:'area+'}"></build-case2-list>
    </div>
</template>
<style>
body {
}
</style>
<script>
import BuildCase2List from './BuildCase2List.vue'
import { mapGetters } from "vuex";
import axios from "axios";

export default {
  data() {
    return {};
  },
  components: {
      BuildCase2List
  }
};
</script>
