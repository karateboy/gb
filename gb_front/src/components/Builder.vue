<template>
    <div>
        <br>
        <div class="panel panel-success">
            <div class="panel-heading">
                {{builder._id}}
            </div>
            <div class="panel-body">
                <table class="table">
                    <tbody>
                        <tr><th>地址</th><td><input type="text" class="form-control" v-model="builder.addr"></td></tr>
                        <tr><th>聯絡人</th><td><input type="text" class="form-control" v-model="builder.contact"></td></tr>
                        <tr><th>電話</th><td>
                            <a :href="'tel:' + builder.phone">{{builder.phone}}</a>
                            <input type="text" class="form-control" v-model="builder.phone"></td></tr>
                    </tbody>
                </table>
                <div class="col-sm-1 col-sm-offset-1">
                    <button class='btn btn-primary' @click='save'>更新資訊</button>
                </div>
            </div>
        </div>
    </div>
</template>
<style scoped>

</style>
<script>
import axios from "axios";
import moment from "moment";
import { mapGetters } from "vuex";

export default {
  props: {
    builder: {
      type: Object,
      required: true
    }
  },
  data() {
    return {};
  },
  computed: {
    ...mapGetters(["user"])
  },
  methods: {
    save() {
      console.log(this.builder);
      axios
        .post("/Builder", this.builder)
        .then(resp => {
          console.log(resp);
          let ret = resp.data;
          if (ret.ok) {
            alert("成功!");
          }
        })
        .catch(err => alert(err));
    }
  },
  components: {}
};
</script>
