<template>
    <div>
        <div class="form-horizontal">
            <div class="form-group">
                <label class="col-lg-2 control-label">起造人名稱:</label>
                <div class="col-lg-10">
                    <input type="text" placeholder="起造人名稱" class="form-control"
                           required v-model="user._id">
                </div>
            </div>
            <div class="form-group">
                <label class="col-lg-2 control-label">地址:</label>
                <div class="col-lg-10">
                    <input type="text" placeholder="地址" class="form-control"
                           required v-model="user.addr">
                </div>
            </div>
            <div class="form-group">
                <label class="col-lg-2 control-label">聯絡人:</label>
                <div class="col-lg-10">
                    <input type="text" placeholder="地址" class="form-control"
                           required v-model="user.contactList[0].contact">
                </div>
            </div>
            <div class="form-group">
                <label class="col-lg-2 control-label">電話:</label>
                <div class="col-lg-10">
                    <input type="text" placeholder="地址" class="form-control"
                           required v-model="user.contactList[0].phone">
                </div>
            </div>

            <div class="form-group">
                <div class="col-lg-offset-2 col-lg-10">
                    <button class="btn btn-primary" @click.prevent="updateBuilder">更新</button>
                </div>
            </div>
            <div class="form-group">
                <div class="col-lg-offset-2 col-lg-10">
                    <button class="btn btn-primary" @click.prevent="giveUpBuilder">找不到</button>
                </div>
            </div>
        </div>
    </div>
</template>
<style>
    body{
        background-color:#ff0000;
    }
</style>
<script>
    import axios from 'axios'
    import { mapActions } from 'vuex'

    export default{
        data(){
            return{
                builder: {}
            }
        },
        computed:{
        },
        methods:{
            updateBuilder(){
                console.log(this.user)
                if(this.user.password != this.user.passwordRetype){
                    alert('密碼不一致')
                    return
                }


                axios.post('/User', this.user).then((resp)=>{
                    const ret = resp.data
                    if(ret.ok)
                        alert("成功")
                    else
                        alert("失敗:" + ret.msg)
                }).catch((err)=>{
                    alert(err)
                })
            }
        },
        components:{
        }
    }
</script>
