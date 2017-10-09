<template>
    <div>
        <br>
        <div class="form-horizontal">
            <div class="form-group"><label class="col-lg-1 control-label">縣市:</label>
                <div class="col-lg-4"><input type="text" class="form-control"
                                             v-model="buildCase.county"></div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">起造人案名:</label>
                <div class="col-lg-4"><input type="text" class="form-control"
                                             v-model="buildCase.name"></div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">建築師:</label>
                <div class="col-lg-4"><input type="text" class="form-control"
                                             v-model="buildCase.architect"></div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">地址:</label>
                <div class="col-lg-4"><input type="text" class="form-control"
                                             v-model="buildCase.addr"></div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">送件日:</label>
                <div class="col-lg-4"><input type="date" class="form-control"
                                             v-model="buildCase.date"></div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">簽約:</label>
                <div class="col-lg-4"><input type="checkbox"
                                             class="form-control"
                                             v-model="buildCase.contracted">
                </div>
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
            buildCase: {
                type: Object,
                required: true
            }
        },
        methods: {
            save() {
                axios.put("/BuildCase", this.buildCase).then(
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
