<template>
    <div>
        <br>
        <div class="form-horizontal">
            <div class="form-group"><label class="col-lg-1 control-label">公立:</label>
                <div class="col-lg-4"><input type="checkbox"
                                             class="form-control"
                                             v-model="careHouse.isPublic">
                </div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">縣市:</label>
                <div class="col-lg-4"><input type="text" class="form-control"
                                             v-model="careHouse.county"></div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">機構名稱:</label>
                <div class="col-lg-4"><input type="text" class="form-control"
                                             v-model="careHouse.name"></div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">負責人:</label>
                <div class="col-lg-4"><input type="text" class="form-control"
                                             v-model="careHouse.principal"></div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">區域:</label>
                <div class="col-lg-4"><input type="text" class="form-control"
                                             v-model="careHouse.district"></div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">地址:</label>
                <div class="col-lg-4"><input type="text" class="form-control"
                                             v-model="careHouse.addr"></div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">電話:</label>
                <div class="col-lg-4"><input type="text" class="form-control"
                                             v-model="careHouse.phone"></div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">床數:</label>
                <div class="col-lg-4"><input type="number" class="form-control"
                                             v-model="careHouse.beds"></div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">廢棄物:</label>
                <div class="col-lg-4"><input type="text" class="form-control"
                                             v-model="careHouse.waste"></div>
            </div>
            <div class="form-group">
                <div class="col-lg-1 col-lg-offset-1">
                    <button class='btn btn-primary' @click='save'>更新</button>
                </div>
            </div>
        </div>
    </div>
</template>
<style scoped>

</style>
<script>
    import axios from 'axios'

    export default {
        props: {
            careHouse: {
                type: Object,
                required: true
            }
        },
        methods: {
            save() {
                axios.put("/CareHouse", this.careHouse).then(
                    resp=>{
                        let ret = resp.data
                        if(ret.Ok){
                            alert("成功!")
                        }
                    }

                ).catch(err=>alert(err))
            }
        },
        components: {
        }
    }
</script>
