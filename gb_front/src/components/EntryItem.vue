<template>
    <div class="modal inmodal" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog modal-lg">
            <div class="modal-content animated fadeIn">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal"><span
                            aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>
                    <h4 class="modal-title">新增</h4>
                </div>
                <div class="modal-body">
                    <form>
                        <div class="form-group">
                            <label class="col-sm-2 control-label">名稱:</label>
                            <div class="col-sm-2"><input type="text" class="form-control"
                                                         v-model="entry.name"></div>
                        </div>
                        <div class="form-group">
                            <label class="col-sm-2 control-label">代碼:</label>
                            <div class="col-sm-2"><input type="text" class="form-control"
                                                         v-model="entry.code"></div>
                        </div>
                        <div class="form-group">
                            <label class="col-sm-2 control-label">頻率:</label>
                            <div class="col-sm-2"><input type="text" class="form-control"
                                                         v-model="entry.freq"></div>
                        </div>

                        <div class="form-group">
                            <label class="col-sm-2 control-label">數量:</label>
                            <div class="col-sm-2"><input type="text" class="form-control"
                                                         v-model.number="entry.volume"></div>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-white" data-dismiss="modal">取消</button>
                    <button type="button" class="btn btn-primary" data-dismiss="modal" @click="addEntry" v-if='opType=="add"'>
                        新增
                    </button>
                    <button type="button" class="btn btn-primary" data-dismiss="modal" @click="updateEntry" v-else>
                        更新
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>
<style>
body {
  background-color: #ff0000;
}
</style>
<script>
export default {
  props: {
    ioType: {
      type: String,
      required: true
    },
    opType: {
      type: String,
      required: true
    },
    entryIndex: {
      type: Number
    },
    entry: {
      type: Object
    }
  },
  data() {
    return {};
  },
  computed: {},
  methods: {
    addEntry() {
      let ret = {
        ioType: this.ioType,
        entry: this.entry
      };
      this.$emit("addEntry", ret);
    },
    updateEntry() {
      this.$emit("updateEntry", {
        ioType: this.ioType,
        entryIndex: this.entryIndex,
        entry: this.entry
      });
    }
  },
  components: {}
};
</script>
